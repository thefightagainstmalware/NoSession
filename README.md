# NoSession
[![modrinth badge](https://modrinth-utils.vercel.app/api/badge/versions?id=Wyj4Bgod&logo=true)](https://modrinth.com/mod/nosession/)<br>
[![curseforge badge](https://img.shields.io/badge/curseforge-1.8.9-F16436?logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/nosession)<br>
![downloads](https://download-counter.thefightagainstmalware.workers.dev/thefightagainstmalware/NoSession?filter=jar)<br>
NoSession is a mod that protects your session ID. 
## Does this make me perfectly safe?
This mod doesn't make you 100% safe, but it makes it much harder to steal your session token. If you want to stay perfectly safe, look at the
[staying safe section](#staying-safe)

## Staying Safe
In order to work around an unpatchable security vulnerability, rename the NoSession jar to !.jar so it can load its protection before any other mods.<br>
You should also use [MultiMC](https://github.com/MultiMC/Launcher/) or one of its derivates [PolyMC](https://github.com/PolyMC/PolyMC) or [PrismLauncher](https://github.com/PrismLauncher/PrismLauncher), because they use launch Minecraft in a way that improves security.<br>
This only protects you from other mods. There are fake verification sites that can steal your session ID through that method.<br>
Don't login with Microsoft OAuth to anything except maybe your Minecraft launcher. You may also want to verify the signature on any NoSession binary. It's signed with [pandaninjas' GPG key](https://raw.githubusercontent.com/pandaninjas/pandaninjas/main/pandaninjas-publickey.key).

See [ILikePlayingGames' SkyblockModSafety guide](https://github.com/ILikePlayingGames/SkyblockModSafety) for other information

## Support
Support is provided in [The Fight Against Malware's discord server](https://discord.gg/TWhrmZFXqb)

## Bug bounty
See [SECURITY.md](SECURITY.md)

## Features
- Does not break existing token login methods
