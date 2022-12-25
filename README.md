# NoSession
NoSession is a mod that protects your session ID. 
## Does this make me perfectly safe?
This mod doesn't make you 100% safe, but it makes it much harder to steal your session token. If you want to stay perfectly safe, look at the
[staying safe section](#staying-safe)

## Staying Safe
In order to work around an unpatchable security vulnerability, rename the NoSession jar to !.jar so it can load its protection before any other mods.
This only protects you from other mods. There are fake verification sites that can steal your session ID through that method. Don't login with Microsoft 
OAuth to anything except maybe your Minecraft launcher.

## Bug bounty
See [SECURITY.MD](security.md)

## Features
- Does not break existing token login methods
