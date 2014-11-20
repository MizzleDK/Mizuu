/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.smbstreamer;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import static com.miz.smbstreamer.Response.HTTP_BADREQUEST;
import static com.miz.smbstreamer.Response.HTTP_INTERNALERROR;

public abstract class StreamServer {

	public static final String MIME_PLAINTEXT = "text/plain";

	private int mTcpPort;
	private final ServerSocket mServerSocket;
	private Thread mServerThread;
	private int mBufferSize = 8192 * 2;

	private static java.text.SimpleDateFormat sGmtFormat;
	static
	{
		sGmtFormat = new java.text.SimpleDateFormat( "E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		sGmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public void setBufferSize(int size) {
		mBufferSize = size;
	}

	/**
	 * Override this to customize the server.<p>
	 *
	 * (By default, this delegates to serveFile() and allows directory listing.)
	 *
	 * @param uri   Percent-decoded URI without parameters, for example "/index.cgi"
	 * @param method        "GET", "POST" etc.
	 * @param parms Parsed, percent decoded parameters from URI and, in case of POST, data.
	 * @param header        Header entries, percent decoded
	 * @return HTTP response, see class Response for details
	 */
	public abstract Response serve(String uri, String method, Properties header, Properties parms, Properties files);

	// ==================================================
	// Socket & server code
	// ==================================================

	/**
	 * Starts a HTTP server to given port.<p>
	 * Throws an IOException if the socket is already in use
	 */

	public StreamServer( int port, File wwwroot ) throws IOException {
		mTcpPort = port;
		mServerSocket = new ServerSocket(mTcpPort);
		mServerThread = new Thread(new Runnable() {
			public void run() {
				try {
					while (true) {
						Socket accept = mServerSocket.accept();
						new HTTPSession(accept);
					}
				} catch (IOException ioe) {}
			}
		});
		mServerThread.setDaemon(true);
		mServerThread.setPriority(Thread.MAX_PRIORITY);
		mServerThread.start();
	}

	/**
	 * Stops the server.
	 */
	public void stop() {
		try {
			mServerSocket.close();
			mServerThread.join();
		} catch (Exception e) {}
	}

	/**
	 * Handles one session, i.e. parses the HTTP request
	 * and returns the response.
	 */
	private class HTTPSession implements Runnable {
		private InputStream is;
		private final Socket socket;

		public HTTPSession(Socket s) {
			socket = s;
			Thread t = new Thread(this);
			t.setDaemon(true);
			t.setPriority(Thread.MAX_PRIORITY);
			t.start();
		}

		public void run() {
			try {
				handleResponse(socket);
			} finally {
				if (is != null) {
					try {
						is.close();
						socket.close();
					} catch(IOException e) {}
				}
			}
		}

		private void handleResponse(Socket socket) {
			try {
				is = socket.getInputStream();
				if ( is == null) return;

				byte[] buf = new byte[mBufferSize];
				int rlen = is.read(buf, 0, mBufferSize);
				if (rlen <= 0) return;

				// Create a BufferedReader for parsing the header.
				ByteArrayInputStream hbis = new ByteArrayInputStream(buf, 0, rlen);
				BufferedReader hin = new BufferedReader( new InputStreamReader( hbis , "utf-8"), mBufferSize);
				Properties pre = new Properties();
				Properties params = new Properties();
				Properties header = new Properties();
				Properties files = new Properties();

				// Decode the header into params and header java properties
				decodeHeader(hin, pre, params, header);
				
				// Logging!
				Log.d("Streamer", pre.toString());
				Log.d("Streamer", "Params: " + params.toString());
				Log.d("Streamer", "Header: " + header.toString());
				
				String method = pre.getProperty("method");
				String uri = pre.getProperty("uri");

				// Ok, now do the serve()
				Response r = serve(uri, method, header, params, files);
				if (r == null)
					sendError(socket, HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: Serve() returned a null response." );
				else
					sendResponse(socket, r.status, r.mimeType, r.header, r.data );

			} catch (IOException ioe) {
				try {
					sendError(socket, HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
				} catch (Throwable t) {}
			} catch (InterruptedException ie) {}  // Thrown by sendError, ignore and exit the thread.

		}

		/**
		 * Decodes the sent headers and loads the data into
		 * java Properties' key - value pairs
		 **/
		private void decodeHeader(BufferedReader in, Properties pre, Properties params, Properties header) throws InterruptedException {
			try {
				// Read the request line
				String inLine = in.readLine();
				if (inLine == null) return;
				StringTokenizer st = new StringTokenizer( inLine );
				if (!st.hasMoreTokens())
					sendError(socket, HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html" );

				String method = st.nextToken();
				pre.put("method", method);

				if (!st.hasMoreTokens())
					sendError(socket, HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html" );

				String uri = st.nextToken();

				// Decode parameters from the URI
				int qmi = uri.indexOf('?');
				if (qmi >= 0) {
					decodeParams(uri.substring(qmi + 1), params);
					uri = decodePercent(uri.substring(0, qmi));
				} else
					uri = Uri.decode(uri);//decodePercent(uri);

				// If there's another token, it's protocol version,
				// followed by HTTP headers. Ignore version but parse headers.
				// NOTE: this now forces header names lowercase since they are
				// case insensitive and vary by client.
				if (st.hasMoreTokens()) {
					String line = in.readLine();
					while (line != null && line.trim().length() > 0) {
						int p = line.indexOf(':');
						if (p >= 0)
							header.put( line.substring(0,p).trim().toLowerCase(Locale.ENGLISH), line.substring(p + 1).trim());
						line = in.readLine();
					}
				}

				pre.put("uri", uri);
			} catch (IOException ioe) {
				sendError(socket, HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
			}
		}

		/**
		 * Decodes the percent encoding scheme. <br/>
		 * For example: "an+example%20string" -> "an example string"
		 */
		private String decodePercent(String str) throws InterruptedException {
			try {
				StringBuffer sb = new StringBuffer();
				for(int i = 0; i < str.length(); i++) {
					char c = str.charAt(i);
					switch (c) {
					case '+':
						sb.append(' ');
						break;
					case '%':
						sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
						i += 2;
						break;
					default:
						sb.append(c);
						break;
					}
				}
				return sb.toString();
			} catch( Exception e ) {
				sendError(socket, HTTP_BADREQUEST, "BAD REQUEST: Bad percent-encoding.");
				return null;
			}
		}

		/**
		 * Decodes parameters in percent-encoded URI-format
		 * ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and
		 * adds them to given Properties. NOTE: this doesn't support multiple
		 * identical keys due to the simplicity of Properties -- if you need multiples,
		 * you might want to replace the Properties with a Hashtable of Vectors or such.
		 */
		private void decodeParams(String params, Properties p) throws InterruptedException {
			if (params == null)
				return;

			StringTokenizer st = new StringTokenizer(params, "&" );
			while (st.hasMoreTokens()) {
				String e = st.nextToken();
				int sep = e.indexOf( '=' );
				if (sep >= 0)
					p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
			}
		}

		/**
		 * Returns an error message as a HTTP response and
		 * throws InterruptedException to stop further request processing.
		 */
		private void sendError(Socket socket, String status, String msg) throws InterruptedException {
			sendResponse(socket, status, MIME_PLAINTEXT, null, null);
			throw new InterruptedException();
		}

		/**
		 * Sends given response to the socket.
		 */
		private void sendResponse(Socket socket, String status, String mime, Properties header, StreamSource data) {
			try {
				if (status == null)
					throw new Error("sendResponse(): Status can't be null.");

				OutputStream out = socket.getOutputStream();
				PrintWriter pw = new PrintWriter(out);
				pw.print("HTTP/1.0 " + status + "\r\n");
				pw.print("Content-Type: video/*\r\n");

				if (header == null || header.getProperty("Date") == null)
					pw.print("Date: " + sGmtFormat.format(new Date()) + "\r\n");

				if (header != null) {
					Enumeration<Object> e = header.keys();
					while (e.hasMoreElements()) {
						String key = (String)e.nextElement();
						String value = header.getProperty(key);
						pw.print(key + ": " + value + "\r\n");
					}
				}

				pw.print("\r\n");
				pw.flush();

				if (data != null) {
					data.open();
					byte[] buff = new byte[mBufferSize];
					int read = 0;
					while ((read = data.read(buff)) > 0) {
						out.write( buff, 0, read );
					}
				}

				out.flush();
				out.close();

				if (data != null)
					data.close();
			} catch (IOException ioe) { // Couldn't write? No can do.
				try { socket.close(); } catch (Throwable t) {}
			}
		}
	}
}