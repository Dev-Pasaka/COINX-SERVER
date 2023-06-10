package com.example.database

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.KMongo

object DatabaseConnection {
        private val client = KMongo.createClient("mongodb+srv://pascarl:pasaka001@coinx.lif2vaj.mongodb.net/")
        val database: MongoDatabase = client.getDatabase("Coinx")

}