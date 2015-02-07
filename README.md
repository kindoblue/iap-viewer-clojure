# iap-viewer


A simple utility to dump the content of an In-App purchase receipt from Apple store backend. The content is validated using a local root CA certificate from Apple.

You can run it as a web application

![screenshot](screenshot.png)

Or you can run at command line

![screenshot](screenshot-1.png)

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

For the webapp, you will need [Bower][2] to install the needed scripts and stylesheets.

The first time you need to run a small script to put in place some needed files, as follow:

    ./install_deps.sh

[1]: https://github.com/technomancy/leiningen
[2]: http://bower.io

## Testing
I've written around 120 sloc for validation of signed data; more than double amount of code for testin it. To run the unit tests

    lein test

## Running

To start a web server for the application, run:

    lein ring server

Or you can simply use the tool as command line utility like this:

    lein run <<your der encoded file>>


## License

Copyright © 2014  Stefano S
