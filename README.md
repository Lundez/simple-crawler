# simple-crawler
A simple crawler in Scala.

Could be prettier. Could be optimized. Could be refactored.  
But it's simple & works ;)

"IntelliJ Web Server" = Press html file in intellij, choose the popup in upper right corner "open in browser".

Simply run either from inside IntelliJ or via `run` in a `sbt`-shell. On my computer it runs ~20s and downloads all of tretton37 (including crawling & large MP4 files).  
The `\r` trick does not seem to work well in `sbt-shell`.

**Architecture**
- `Main.scala` this is where the magic happens.
  1. Fetch & Save Index
  2. Clean html & fetch all links
  3. For each link
    - Clean html & fetch all links that hasn't been fetched
    - Repeat
  4. Shut down once there's no more new links
- `LinkHandler.scala` this object validates if link should be crawled and fetches all assets links inside a html-document
- `HtmlCleaner.scala` this object cleans html doc to make the asset paths look better
  - Removes cache path (`X.css?{hash}`)
  - Relativize path (e.g. `../asset`, `asset`)
    - Because intellij built-in web server does not work with `/asset`.
  - Adds `.html` to "internal" html links to also be able to traverse them in IntelliJ's web server
- `FileHandler.scala` this object takes the url & byte-content (post-cleaning) and saves to file-system at the correct location.
- `ResponseData.scala` is a `case class` that makes the code a little cleaner

**Known Issues**
- There's a few href/src inside the CSS that I have been to lazy to fix.
    - This means that fonts don't load, e.g. we don't get icon-fonts.
- The `data-videosrc` attribute does not seem to work, not sure how it's supposed to work. The file refered in `src` does exist on file system :)
- Analytics is on a https and not allowed to fetch.
