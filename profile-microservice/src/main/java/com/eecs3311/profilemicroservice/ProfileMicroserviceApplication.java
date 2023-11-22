package com.eecs3311.profilemicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;

@SpringBootApplication
public class ProfileMicroserviceApplication {
	public static String dbUri = "bolt://localhost:7687";
	public static Config config = Config.builder().withoutEncryption().build();
    public static Driver driver = GraphDatabase.driver(dbUri, AuthTokens.basic("neo4j","12345678"), config);

    
	public static void main(String[] args) {
		SpringApplication.run(ProfileMicroserviceApplication.class, args);
		
		ProfileDriverImpl.InitProfileDb();
		PlaylistDriverImpl.InitPlaylistDb();
		
		System.out.println("Profile service is running on port 3002");
	}
}

