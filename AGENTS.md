# **The following prompts are for all Tasks**
- Always format jsons in a good looking way, make sure they look like vscodes pretty printed jsons.
 ---
# **The following prompts are for Minecraft Modding tasks only:**
- Make sure to use and expand API features for the things you make if thats needed. Make sure to tell this the one who prompts and when they tell you to not modify the API, then don‘t modify it.
- Make sure to check Minecrafts Source Code for injecting etc. to make sure your not targeting non-existent functions or variables.
- Make sure to not use deprecated features from Minecraft or Fabric API
- Make sure that Vanilla features still work!
- You can use `./gradlew runClient` to test if Minecraft starts, which you should also do btw and `./gradlew runClient --warning-mode all` is a more detailed .
- If you add new tags, then translate them in en\_us to avoid a fabric api warning.

 --- 
# **The following prompts are for working on ./blob.ps1 only**
- Make sure to expand API features and put functions which you think can be reused in other commands too, then make them a reusable api module(s), or if they are not deemed as reusable, then put them in separate modules for that specific command.
- Paths should always be configurable, unless specified by the prompter!
- If your working with API keys, tokens, etc. anything that is obviously supposed to stay private, make sure to NOT LEAK IT, DO NOT SAVE IT ANYWHERE WHERE IT‘LL END UP ON GITHUB. And always check if that may have already been done on accident and report the prompter that their might be leaked API keys.
- `blobManager/packages/config/keys/personal.txt` is for 