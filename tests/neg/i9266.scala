//> using options -Xfatal-warnings

import language.`future-migration`

def test = { implicit x: Int => x + x } // error
