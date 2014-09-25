/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.datagrid.hotrod;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

/**
 * @author Martin Gencur
 */
public class FootballManager {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    private static final String JDG_HOST = "jdg.host";
    private static final String HOTROD_PORT = "jdg.hotrod.port";
    private static final String I_7_NANI = "jdg.7nani";
    private static final String PROPERTIES_FILE = "jdg.properties";
    private static final String msgTeamMissing = "The specified team \"%s\" does not exist, choose next operation\n";
    private static final String msgEnterTeamName = "Enter team name: ";
    private static final String msgEnterResidentialCf = "Enter residential cf: ";
    private static final String msgEnterResidentialPt = "Enter residential pushToken: ";
    private static final String msgEnterResidentialLifespanValue = "Enter entry lifespan (seconds): ";
    private static final String msgMassiveResidentialInsert = "Enter number of entries: ";
    private static final String initialPrompt = "Choose action:\n" + "============= \n" + "at  -  add a team\n"
            + "help  -  print this menu\n" + "ap  -  add a player to a team\n" + "rt  -  remove a team\n" + "rp  -  remove a player from a team\n"
            + "mr  -  Massive insert in cache\n" + "mrm  -  Massive mortal insert in cache\n"
            + "ar  -  add a residential user\n" + "arm  -  add a residential mortal user\n"
            + "rr  -  remove a residential user\n" + "rR  -  remove all residentials users\n"
            + "pr  -  print a residential user\n" + "pR  -  print all residential users\n"
            + "p   -  print all teams and players\n"+ "nani  -  add 7 nani \n" + "q   -  quit\n";
    private static final String teamsKey = "teams";

    private Console con;
    private RemoteCacheManager cacheManager;
    private RemoteCache<String, Object> cache;
    private RemoteCache<String, Object> cacheResidential;

    public FootballManager(Console con) {
        this.con = con;
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
              .host(jdgProperty(JDG_HOST))
              .port(Integer.parseInt(jdgProperty(HOTROD_PORT)));
        cacheManager = new RemoteCacheManager(builder.build());
        cache = cacheManager.getCache("teams");
        cacheResidential = cacheManager.getCache("residentials");

        if(!cache.containsKey(teamsKey)) {
            List<String> teams = new ArrayList<String>();
            Team t = new Team("Barcelona");
            t.addPlayer("Messi");
            t.addPlayer("Pedro");
            t.addPlayer("Puyol");
            cache.put(t.getName(), t);
            teams.add(t.getName());
            cache.put(teamsKey, teams);
        }
    }

    public void addTeam() {
        String teamName = con.readLine(msgEnterTeamName);
        @SuppressWarnings("unchecked")
        List<String> teams = (List<String>) cache.get(teamsKey);
        if (teams == null) {
            teams = new ArrayList<String>();
        }
        Team t = new Team(teamName);
        cache.put(teamName, t);
        teams.add(teamName);
        // maintain a list of teams under common key
        cache.put(teamsKey, teams);
    }

    public void addResidential(boolean immortal) {
        String cf = con.readLine(msgEnterResidentialCf);
        String pt = con.readLine(msgEnterResidentialPt);
        @SuppressWarnings("unchecked")

        PushInfoResidential user = new PushInfoResidential(cf,pt);
        System.out.println(user.getCf());
        System.out.println(user.getPushToken());
        if(immortal){
            cacheResidential.put(cf, user);
        } else {
            long lifespan;
            try{
                lifespan = Long.parseLong(con.readLine(msgEnterResidentialLifespanValue));
            } catch(Throwable t){
                System.out.println("Invalid value; used default 200s instead...");
                lifespan = 200L;
            }
            cacheResidential.put(cf, user, lifespan, TimeUnit.SECONDS);
        }
    }

    public boolean removeResidential() {
        con.printf("Remove Residential User Info\n");
        String cf = con.readLine(msgEnterResidentialCf);
        PushInfoResidential toRemove = null;
        if(cacheResidential.containsKey(cf)){
            toRemove = (PushInfoResidential)cacheResidential.get(cf);
            cacheResidential.remove(toRemove);
            con.printf("entry removed fom cache\n");
        }
        return toRemove!=null;
    }

    public boolean removeResidentials() {
        con.printf("Remove Residentials Users Info\n");
        boolean done = false;
        try {
            cacheResidential.clear();
            done = true;
        } catch (Throwable t) {
            con.printf("Error removing residential");
        }
        return done;
    }

    public void massiveResidentialInsert(boolean immortal) {

        int entries;
        long lifespan=0L;

        try {
            entries = Integer.parseInt(con.readLine(msgMassiveResidentialInsert));
            if(!immortal)
                lifespan = Long.parseLong(con.readLine(msgEnterResidentialLifespanValue));
        } catch (Throwable t) {
            entries = 100;
            if(!immortal)
                lifespan = 200L;
        }
        con.printf("Inserting " + entries + " entries in residentials cache\n");
        if(!immortal)
            con.printf("with lifespan = " + lifespan + "\n");

        if(immortal){
            for(int i = 0; i<entries; i++){
                String tmpCf = getRandomResidentialUserCf();
                cacheResidential.put(tmpCf,new PushInfoResidential(tmpCf,getRandomResidentialUserToken()));
            }
        } else {
            for(int i = 0; i<entries; i++){
                String tmpCf = getRandomResidentialUserCf();
                cacheResidential.put(tmpCf,new PushInfoResidential(tmpCf,getRandomResidentialUserToken()), lifespan, TimeUnit.SECONDS);
            }
        }
    }

    public void insert7nani() {

        String[] nani = jdgProperty(I_7_NANI).split(",");
        int entries = nani.length;

        con.printf("Inserting " + entries + " nani in residentials cache\n");

        for(int i = 0; i<entries; i++){
            String tmpCf = nani[i];
            cacheResidential.put(tmpCf,new PushInfoResidential(tmpCf,getRandomResidentialUserToken()));
        }
    }

    private String getRandomResidentialUserCf() {
        return RandomStringUtils.randomAlphanumeric(16);
    }

    private String getRandomResidentialUserToken() {
        return UUID.randomUUID().toString();
    }

    public void addPlayers() {
        String teamName = con.readLine(msgEnterTeamName);
        String playerName = null;
        Team t = (Team) cache.get(teamName);
        if (t != null) {
            while (!(playerName = con.readLine("Enter player's name (to stop adding, type \"q\"): ")).equals("q")) {
                t.addPlayer(playerName);
            }
            cache.put(teamName, t);
        } else {
            con.printf(msgTeamMissing, teamName);
        }
    }

    public void removePlayer() {
        String playerName = con.readLine("Enter player's name: ");
        String teamName = con.readLine("Enter player's team: ");
        Team t = (Team) cache.get(teamName);
        if (t != null) {
            t.removePlayer(playerName);
            cache.put(teamName, t);
        } else {
            con.printf(msgTeamMissing, teamName);
        }
    }

    public void removeTeam() {
        String teamName = con.readLine(msgEnterTeamName);
        Team t = (Team) cache.get(teamName);
        if (t != null) {
            cache.remove(teamName);
            @SuppressWarnings("unchecked")
            List<String> teams = (List<String>) cache.get(teamsKey);
            if (teams != null) {
                teams.remove(teamName);
            }
            cache.put(teamsKey, teams);
        } else {
            con.printf(msgTeamMissing, teamName);
        }
    }

    public void printTeams() {
        @SuppressWarnings("unchecked")
        List<String> teams = (List<String>) cache.get(teamsKey);
        if (teams != null) {
            for (String teamName : teams) {
                con.printf(cache.get(teamName).toString());
            }
        }
    }

    public void printResidential() {
        String cf = con.readLine(msgEnterResidentialCf);
        PushInfoResidential tmp = null;
        if(cacheResidential.containsKey(cf)){
            tmp = (PushInfoResidential) cacheResidential.get(cf);
            con.printf(tmp.toString());
        }
    }

    public void printResidentials() {
        @SuppressWarnings("unchecked")
        Set<String> usersIds = (Set<String>) cacheResidential.keySet();
        if (usersIds != null) {
            con.printf("KeySet size: " + usersIds.size() + "\n");
            int count = 1;
            for (String user : usersIds) {
                con.printf(ANSI_GREEN + count +": User " + user + "\n");
                PushInfoResidential tmp = null;
                try{
                    tmp = (PushInfoResidential) cacheResidential.get(user);
                    con.printf(ANSI_RESET + tmp.toString());
                } catch (Throwable t){
                    System.out.println(ANSI_RED + "Error retrieving user #" + count +" with cf:" + user + ANSI_RED);
                    System.out.println(ANSI_RESET+"*************"+ANSI_RESET);
                }
                count++;
            }
        }
    }

    public void stop() {
        cacheManager.stop();
    }

    public static void main(String[] args) {
        Console con = System.console();
        FootballManager manager = new FootballManager(con);
        con.printf(initialPrompt);

        while (true) {
            String action = con.readLine(">");
            if ("at".equals(action)) {
                manager.addTeam();
            } else if ("ap".equals(action)) {
                manager.addPlayers();
            } else if ("rt".equals(action)) {
                manager.removeTeam();
            } else if ("rp".equals(action)) {
                manager.removePlayer();
            } else if ("p".equals(action)) {
                manager.printTeams();
            } else if ("ar".equals(action)) {
                manager.addResidential(true);
            } else if ("arm".equals(action)) {
                manager.addResidential(false);
            } else if ("pR".equals(action)) {
                manager.printResidentials();
            } else if ("pr".equals(action)) {
                manager.printResidential();
            } else if ("rr".equals(action)) {
                con.printf("status:" + manager.removeResidential());
            } else if ("rR".equals(action)) {
                con.printf("status:" + manager.removeResidentials());
            } else if ("nani".equals(action)) {
                manager.insert7nani();
            } else if ("mr".equals(action)) {
                manager.massiveResidentialInsert(true);
            } else if ("mrm".equals(action)) {
                manager.massiveResidentialInsert(false);
            } else if ("help".equals(action)) {
                con.printf(initialPrompt);
            } else if ("q".equals(action)) {
                manager.stop();
                break;
            }
        }
    }

    public static String jdgProperty(String name) {
        Properties props = new Properties();
        try {
            props.load(FootballManager.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return props.getProperty(name);
    }
}
