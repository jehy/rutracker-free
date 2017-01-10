# rutracker-free
Android thin client for rutracker.org.
Version 9.0 implemented TOR
(using my [own library distribution](https://github.com/jehy/Tor-Onion-Proxy-Library))
instead of Google Compresssion Proxy at least.
It is a bit slow but should be usable from any location.

Rutracker-free on different resources:

* [4pda](http://4pda.ru/forum/index.php?showtopic=733085)
* [habrahabr - google compression proxy](https://habrahabr.ru/post/279267/)
* [habrahabr - tor](https://habrahabr.ru/post/313030/)
* [rutracker](http://rutracker.org/forum/viewtopic.php?t=5191131)

Old branch using Google compression proxy can be found
 [here.](https://github.com/jehy/rutracker-free/tree/old/GCP)

#Requirements
To build project, you will need:
* Android Studio 2.1+

#Build instructions
* If you are making a **release** build, add your own [fabric](https://fabric.io) key to application manifest in `application` block like this:
```xml
<meta-data
  android:name="io.fabric.ApiKey"
  android:value="xxx"
  />
```