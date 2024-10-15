package config

import java.io.FileInputStream
import java.util.Properties

class Config {
    val pgHost: String
    val pgUser: String
    val pgPassword: String
    val pgDatabase: String

    init {
        val props = Properties()
        props.load(FileInputStream("env.properties"))

        pgHost = props.getProperty("pg.host")
        pgUser = props.getProperty("pg.user")
        pgPassword = props.getProperty("pg.password")
        pgDatabase = props.getProperty("pg.database")
    }
}
