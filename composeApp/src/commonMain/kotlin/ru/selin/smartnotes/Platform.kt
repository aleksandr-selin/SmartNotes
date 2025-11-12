package ru.selin.smartnotes

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform