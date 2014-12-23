Translating Mizuu
============

#### Steps

1. [Fork Mizuu][1].
2. Read the guidelines on how to translate strings below.
3. `git checkout -b branch-name-language dev` and commit your translations.
4. When you think your translations are done, start a pull request against the dev branch.

#### How to translate

1. Locate the original strings in [/res/values/strings.xml][2]
2. Check if the strings have already been translated into the language you want to translate to. You can do that by checking [/res/][3] folder and look for values-##, where ## represents the [ISO 639-1 code][4] of the language, i.e. `values-da` for Danish.
3. If the language folder exists, you can check previous translations and add missing ones. If the folder doesn't exist, you can create it and add translations.
4. See the section below for specific translation examples

Examples of string translation
============

#### Regular strings

`<string name="chooserMovies">Movies</string>`

*... gets translated into this (for Danish):*

`<string name="chooserMovies">Film</string>`

Some strings have explanations / context descriptions. Please refer to these, if you have any questions or doubts, or alternatively contact me or mention it in the pull request.

#### Plural strings

```xml
<plurals name="moviesInLibrary">
    <item quantity="one">movie</item>
    <item quantity="other">movies</item>
</plurals>
```

*... gets translated into this (for Danish):*

```xml
<plurals name="moviesInLibrary">
    <item quantity="one">film</item>
    <item quantity="other">film</item>
</plurals>
```

Some languages include words for many different quantities. You may add or remove these quantities to match the specific language. Valid quantities are: "zero", "one", "two", "few", "many", "other".

 [1]: https://github.com/MizzleDK/Mizuu/fork
 [2]: https://github.com/MizzleDK/Mizuu/blob/michell-dev/app/src/main/res/values/strings.xml
 [3]: https://github.com/MizzleDK/Mizuu/blob/michell-dev/app/src/main/res/
 [4]: http://www.loc.gov/standards/iso639-2/php/code_list.php
