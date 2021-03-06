package ws.nmathe.saber.core.schedule;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Role;
import org.apache.commons.lang3.StringUtils;
import ws.nmathe.saber.Main;
import net.dv8tion.jda.core.entities.Message;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.*;
import java.util.List;

/**
 *  This class is responsible for both parsing a discord message containing information that creates a ScheduleEvent,
 *  as well as is responsible for generating the message that is to be used as the parsable discord schedule entry
 */
public class MessageGenerator
{
    static Message generate(String title, ZonedDateTime start, ZonedDateTime end, List<String> comments,
                            int repeat, String url, List<Date> reminders, Integer eId, String cId, String guildId,
                            List<String> rsvpYes, List<String> rsvpNo, List<String> rsvpUndecided)
    {
        String titleUrl = url != null ? url : "https://nmathe.ws/bots/saber";
        String titleImage = "https://upload.wikimedia.org/wikipedia/en/8/8d/Calendar_Icon.png";
        String footerStr = "ID: " + Integer.toHexString(eId);

        // generate reminder footer
        if (!reminders.isEmpty())
        {
            footerStr += " | remind in ";
            long minutes = Instant.now().until(reminders.get(0).toInstant(), ChronoUnit.MINUTES);
            if(minutes<=120)
                footerStr += " " + minutes + "m";
            else
                footerStr += " " + (int) Math.ceil(minutes/60) + "h";
            for (int i=1; i<reminders.size()-1; i++)
            {
                minutes = Instant.now().until(reminders.get(i).toInstant(), ChronoUnit.MINUTES);
                if(minutes<=120)
                    footerStr += ", " + minutes + "m";
                else
                    footerStr += ", " + (int) Math.ceil(minutes/60) + "h";
            }
            if (reminders.size()>1)
            {
                minutes = Instant.now().until(reminders.get(reminders.size()-1).toInstant(), ChronoUnit.MINUTES);
                footerStr += " and ";
                if(minutes<=120)
                    footerStr += minutes + "m";
                else
                    footerStr += (int) Math.ceil(minutes/60) + "h";
            }
        }

        // get embed color from first hoisted bot role
        Color color = Color.DARK_GRAY;
        List<Role> roles = new ArrayList<>(Main.getBotJda().getGuildById(guildId)
                .getMember(Main.getBotJda().getSelfUser()).getRoles());
        while(!roles.isEmpty())
        {
            if(roles.get(0).isHoisted())
            {
                color = roles.get(0).getColor();
                break;
            }
            else
            {
                roles.remove(0);
            }
        }

        /*  Invert color if event has started
        if(Instant.now().isAfter(start.toInstant()))
        {
            color = new Color(255-color.getRed(), 255-color.getGreen(), 255-color.getBlue());
        }
        */

        String bodyContent;
        if(Main.getScheduleManager().getStyle(cId).toLowerCase().equals("narrow"))
            bodyContent = generateBodyNarrow(start, end, cId, repeat, rsvpYes, rsvpNo, rsvpUndecided);
        else
            bodyContent = generateBodyFull(start, end, cId, repeat, comments, rsvpYes, rsvpNo, rsvpUndecided);


        EmbedBuilder builder = new EmbedBuilder();
        builder.setDescription(bodyContent)
                .setColor(color)
                .setAuthor(title, titleUrl, titleImage)
                .setFooter(footerStr, null);

        return new MessageBuilder().setEmbed(builder.build()).build();
    }

    private static String generateBodyFull(ZonedDateTime eStart, ZonedDateTime eEnd, String cId,
                                           int eRepeat, List<String> eComments, List<String> rsvpYes,
                                           List<String> rsvpNo, List<String> rsvpUndecided)
    {
        String timeFormatter;
        if(Main.getScheduleManager().getClockFormat(cId).equals("24"))
            timeFormatter = "H:mm";
        else
            timeFormatter = "h:mm a";

        // the 'actual' first line (and last line) define format
        String msg = "";

        String dash = "\u2014";
        String timeLine = "< " + eStart.format(DateTimeFormatter.ofPattern("MMM d"));
        if( eStart.until(eEnd, ChronoUnit.SECONDS)==0 )
        {
            timeLine += ", " + eStart.format(DateTimeFormatter.ofPattern(timeFormatter)) + " >\n";
        }
        else if( eStart.until(eEnd, ChronoUnit.DAYS)>=1 )
        {
            if( eStart.toLocalTime().equals(LocalTime.MIN) && eStart.toLocalTime().equals(LocalTime.MIN) )
                timeLine += " " + dash + " " + eEnd.format(DateTimeFormatter.ofPattern("MMM d")) + " >\n";
            else
                timeLine += ", " + eStart.format(DateTimeFormatter.ofPattern(timeFormatter)) +
                        " " + dash + " " + eEnd.format(DateTimeFormatter.ofPattern("MMM d")) + ", " +
                        eEnd.format(DateTimeFormatter.ofPattern(timeFormatter)) + " >\n";
        }
        else
        {
            timeLine += ", " + eStart.format(DateTimeFormatter.ofPattern(timeFormatter)) +
                    " " + dash + " " + eEnd.format(DateTimeFormatter.ofPattern(timeFormatter)) + " >\n";
        }

        String repeatLine = "> " + getRepeatString( eRepeat, false ) + "\n";
        String zoneLine = "[" + eStart.getZone().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "]" +
                MessageGenerator.genTimer(eStart,eEnd) + "\n";

        msg += "```Markdown\n\n" + timeLine + repeatLine + "```\n";

        // insert each comment line with a gap line
        for( String comment : eComments )
        {
            // code blocks in comments must be close within the comment
            int code = StringUtils.countMatches("```", comment);
            if((code%2) == 1)
            {
                msg += comment + " ```" + "\n";
            }
            else
            {
                msg += comment + "\n\n";
            }
        }

        // if rsvp is enabled, show the number of rsvp
        if(rsvpYes != null && rsvpNo != null)
        {
            String rsvpLine = "- RSVP: <Yes " + rsvpYes.size() + "> <No " + rsvpNo.size()
                    + "> <Undecided " + rsvpUndecided.size() + ">\n";
            msg += "```Markdown\n\n" + zoneLine + rsvpLine + "```";
        }
        else
        {
            msg += "```Markdown\n\n" + zoneLine + "```";
        }

        return msg;
    }

    private static String generateBodyNarrow(ZonedDateTime eStart, ZonedDateTime eEnd, String cId,
                                           int eRepeat, List<String> rsvpYes,
                                           List<String> rsvpNo, List<String> rsvpUndecided)
    {
        // determine the formatting for clock
        String timeFormatter;
        if(Main.getScheduleManager().getClockFormat(cId).equals("24"))
            timeFormatter = "H:mm";
        else
            timeFormatter = "h:mm a";

        // create the first line of the body
        String dash = "\u2014";
        String timeLine = "< " + eStart.format(DateTimeFormatter.ofPattern("MMM d"));
        if( eStart.until(eEnd, ChronoUnit.SECONDS)==0 )
        {
            timeLine += ", " + eStart.format(DateTimeFormatter.ofPattern(timeFormatter)) + " >\n";
        }
        else if( eStart.until(eEnd, ChronoUnit.DAYS)>=1 )
        {
            if( eStart.toLocalTime().equals(LocalTime.MIN) && eStart.toLocalTime().equals(LocalTime.MIN) )
                timeLine += " " + dash + " " + eEnd.format(DateTimeFormatter.ofPattern("MMM d")) + " >\n";
            else
                timeLine += ", " + eStart.format(DateTimeFormatter.ofPattern(timeFormatter)) +
                        " " + dash + " " + eEnd.format(DateTimeFormatter.ofPattern("MMM d")) + ", " +
                        eEnd.format(DateTimeFormatter.ofPattern(timeFormatter)) + " >\n";
        }
        else
        {
            timeLine += ", " + eStart.format(DateTimeFormatter.ofPattern(timeFormatter)) +
                    " " + dash + " " + eEnd.format(DateTimeFormatter.ofPattern(timeFormatter)) + " >\n";
        }

        // create the second line of the body
        String lineTwo = "[" + eStart.getZone().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) +
                "](" + getRepeatString( eRepeat, true ) + ")";

        // if rsvp is enabled, show the number of rsvps
        if(rsvpYes != null && rsvpNo != null)
            lineTwo += " <Y " + rsvpYes.size() + "> <N " + rsvpNo.size() + "> <U " + rsvpUndecided.size() + ">\n";
        else
            lineTwo += "\n";

        return "```Markdown\n\n" + timeLine + lineTwo + "```\n";
    }

    public static String getRepeatString(int bitset, boolean isNarrow)
    {
        String str;
        if(isNarrow)
        {
            if( bitset == 0 )
                return "once";
            if( bitset == 0b1111111 || bitset == 0b10000001)
                return "every day";

            // repeat on interval
            if((bitset & 0b10000000) == 0b10000000 )
            {
                Integer interval = (0b10000000 ^ bitset);
                String[] spellout = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
                return "every " + (interval>spellout.length ? interval : spellout[interval-1]) + " days";
            }

            // repeat on fixed days
            str = "every ";
        }
        else
        {
            if( bitset == 0 )
                return "does not repeat";
            if( bitset == 0b1111111 )
                return "repeats daily";

            // repeat on interval
            if((bitset & 0b10000000) == 0b10000000 )
            {
                Integer interval = (0b10000000 ^ bitset);
                String[] spellout = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
                return "repeats every " + (interval>spellout.length ? interval : spellout[interval-1]) + " days";
            }

            // repeat on fixed days
            str = "repeats weekly on ";
        }

        if( (bitset & 1) == 1 )
        {
            if(bitset==0b0000001)
                return str + "Sunday";

            str += "Sun";
            if( (bitset>>1) != 0 )
                str += ", ";
        }
        bitset = bitset>>1;
        if( (bitset & 1) == 1 )
        {
            if(bitset==0b0000010)
                return str + "Monday";

            str += "Mon";
            if( (bitset>>1) != 0 )
                str += ", ";
        }
        bitset = bitset>>1;
        if( (bitset & 1) == 1 )
        {
            if(bitset==0b0000100)
                return str + "Tuesday";

            str += "Tue";
            if( (bitset>>1) != 0 )
                str += ", ";
        }
        bitset = bitset>>1;
        if( (bitset & 1) == 1 )
        {
            if(bitset==0b0001000)
                return str + "Wednesday";

            str += "Wed";
            if( (bitset>>1) != 0 )
                str += ", ";
        }
        bitset = bitset>>1;
        if( (bitset & 1) == 1 )
        {
            if(bitset==0b0010000)
                return str + "Thursday";

            str += "Thu";
            if( (bitset>>1) != 0 )
                str += ", ";
        }
        bitset = bitset>>1;
        if( (bitset & 1) == 1 )
        {
            if(bitset==0b0100000)
                return str + "Friday";

            str += "Fri";
            if( (bitset>>1) != 0 )
                str += ", ";
        }
        bitset = bitset>>1;
        if( (bitset & 1) == 1 )
        {
            if(bitset==0b1000000)
                return str + "Saturday";

            str += "Sat";
            if( (bitset>>1) != 0 )
                str += ", ";
        }
        return str;
    }

    private static String genTimer(ZonedDateTime start, ZonedDateTime end)
    {
        String timer;
        long timeTilStart = ZonedDateTime.now().until(start, ChronoUnit.SECONDS);
        long timeTilEnd = ZonedDateTime.now().until(end, ChronoUnit.SECONDS);

        if(start.isAfter(ZonedDateTime.now()))
        {
            timer = "(begins ";
            if( timeTilStart < 60 * 60 )
            {
                int minutesTil = (int)Math.ceil((double)timeTilStart/(60));
                if( minutesTil <= 1)
                    timer += "in a minute.)";
                else
                    timer += "in " + minutesTil + " minutes.)";
                //timer += "within the hour.)";
            }
            else if( timeTilStart < 24 * 60 * 60 )
            {
                int hoursTil = (int)Math.ceil((double)timeTilStart/(60*60));
                if( hoursTil <= 1)
                    timer += "within the hour.)";
                else
                    timer += "in " + hoursTil + " hours.)";
            }
            else
            {
                int daysTil = (int) ChronoUnit.DAYS.between(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS),
                        start.truncatedTo(ChronoUnit.DAYS));
                if( daysTil <= 1)
                    timer += "tomorrow.)";
                else
                    timer += "in " + daysTil + " days.)";
            }
        }
        else // if the event has started
        {
            timer = "(ends ";
            if( timeTilEnd < 60*60 )
            {
                int minutesTil = (int)Math.ceil((double)timeTilEnd/(60));
                if( minutesTil <= 1)
                    timer += "in a minute.)";
                else
                    timer += "in " + minutesTil + " minutes.)";
                //timer += "within one hour.)";
            }

            else if( timeTilEnd < 24 * 60 * 60 )
            {
                int hoursTil = (int)Math.ceil((double)timeTilEnd/(60*60));
                if( hoursTil <= 1)
                    timer += "within one hour.)";
                else
                    timer += "in " + hoursTil + " hours.)";
            }
            else
            {
                int daysTil = (int) ChronoUnit.DAYS.between(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS), end.truncatedTo(ChronoUnit.DAYS));
                if( daysTil <= 1)
                    timer += "tomorrow.)";
                else
                    timer += "in " + daysTil + " days.)";
            }
        }
        return timer;
    }
}
