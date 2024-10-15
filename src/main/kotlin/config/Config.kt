package config

import java.io.FileInputStream
import java.util.Properties

class Config {
    val httpPort: Int
    val pgHost: String
    val pgPort: Int
    val pgUser: String
    val pgPassword: String
    val pgDatabase: String

    init {
        val props = Properties()
        props.load(FileInputStream("env.properties"))

        httpPort = props.getProperty("http.port").toInt()
        pgHost = props.getProperty("pg.host")
        pgPort = props.getProperty("pg.port").toInt()
        pgUser = props.getProperty("pg.user")
        pgPassword = props.getProperty("pg.password")
        pgDatabase = props.getProperty("pg.database")
    }
}
