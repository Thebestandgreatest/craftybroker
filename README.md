A broker for [Impulse](https://github.com/Arson-Club/Impulse) to interact with [Crafty Controller](https://gitlab.com/crafty-controller/crafty-4) using the api

Currently only able to be used in insecure mode

<h1>Installation</h1>
Download the latest release from the releases page, and put the jar in the plugins/impulse directory, should be the same directory as the config, then change the config to allow the broker to stop and start your servers

<h1>Usage</h1>

Example Config:
```yaml
servers: 
   name: "lobby"
   type: "crafty"
   crafty:
     serverID: "1111111111"
     token: "asdfjklasfdjklasdfjklasdjfklasdjfklasdjfkl"
     craftyAddress: "https://localhost:8443"
     insecureMode: true
```
</p>
Explanation of config:

```yaml
servers:
  name: "lobby"
  type: "crafty"
```
Required by Impulse
Need to set crafty as the type

```yaml
crafty:
  serverID: "111111111111"
  token: ""
  craftyAddress: "https://localhost:8443"
  insecureMode: true
```
<ul>
  <li><b>serverID:</b> found in the url of the server as you are on the page in the crafty web ui, also listed just below the name of the server as UUID</li>
  <li><b>token:</b> found by making an API Key in the crafty settings page where you manage users, click on a user, click on api keys, and make a new one with the COMMANDS permission then scroll the new table entry over until you see get token. Use that as your token. Make sure that the user has access to the servers that Impulse will manage</li>
  <li><b>craftyAddress</b> address where you access the crafty controller web ui, defaults to https://localhost:8443</li>
  <li><b>insecureMode</b> use to disable certificate checking when sending api requests, <b>KNOW WHAT YOU ARE DOING IF YOU ENABLE THIS</b>, temporary workaround, defaults to false</li>
</ul>
