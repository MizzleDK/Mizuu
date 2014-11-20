Contributing to Mizuu's development
============

#### Contributing code?

1. [Fork Mizuu][1].
2. `git checkout -b descriptive-branch-name dev` and make your commits.
3. When you think your code is ready, start a pull request against the dev branch. Please reference [existing issues][2] when possible.

#### No code?
* You can [suggest features][2].
* You can [discuss a bug][2] or [report a bug][2]!
* You can [translate strings][3].

Branch structure
----------------

The repository is made up of two main branches: master (stable) and dev (unstable / work in progress).

* **master** has the latest stable code, its tags are released as versions of Mizuu on GitHub, www.mizuu.tv and Google Play.
* **dev** includes the latest unstable code from contributers (you!).

Since Mizuu was just recently open-sourced, there's still some work to do with branching and the overall structure of the development, but this is on the to-do list. For now, use the tagged commits as release versions.

Setup
-----

Mizuu is developed using Android Studio and is built with Java 1.7.

In order to use the project with the various web services, you'll need to add an `api_keys.xml` file to `/res/values/` with the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="tmdb_api_key">add_your_own</string>
    <string name="tvdb_api_key">add_your_own</string>
    <string name="youtube_api_key">add_your_own</string>
    <string name="trakt_api_key">add_your_own</string>
</resources>
```

 [1]: https://github.com/MizzleDK/Mizuu/fork
 [2]: https://github.com/MizzleDK/Mizuu/issues
 [3]: http://translate.mizuu.tv/
