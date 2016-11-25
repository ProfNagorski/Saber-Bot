package io.schedulerbot.utils;

/**
 * file: BotConfig.java
 * contains configurable variables for the bot
 */
public class BotConfig
{
    public static String TOKEN = "MjUwODAxNjAzNjMwNTk2MTAw.CxaJqw.IPLbPDoRhVBZhZyQ9JjmoKvsskE" ;     // your bot's token
    public static String PREFIX = "!";                  // prefix bot-sama should listen for
    public static String ANNOUNCE_CHAN = "announce";     // the channel name where bot announces events too
                                                        // if ANNOUNCE_CHAN is empty, defaults to default channel
    public static String EVENT_CHAN = "event_schedule"; // the channel in which bot manages scheduled events
    public static String CONTROL_CHAN = "bot_control";         // the channel where bot listens for commands, if set
                                                        // to an empty string bot listens everywhere
}