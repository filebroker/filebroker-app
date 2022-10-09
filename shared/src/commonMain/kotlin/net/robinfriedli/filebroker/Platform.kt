package net.robinfriedli.filebroker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform