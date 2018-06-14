package de.rwth.comsys.cloudanalyzer.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class RegexList
{

    private HashMap<String, List<Regex>> regexList;

    public RegexList()
    {
        regexList = new HashMap<>();
    }

    public void addRegex(String regex, int region, String type, boolean ignoreCase)
    {
        List<Regex> rl = regexList.get(type);
        if (rl == null)
        {
            rl = new ArrayList<>();
            regexList.put(type, rl);
        }
        rl.add(new Regex(regex, region, type, ignoreCase));
    }

    public Regex matches(String str)
    {
        for (List<Regex> rl : regexList.values())
        {
            Regex r = matches(rl, str);
            if (r != null)
                return r;
        }
        return null;
    }

    public Regex matches(String str, String type)
    {
        List<Regex> rl = regexList.get(type);
        return matches(rl, str);
    }

    private Regex matches(List<Regex> rl, String str)
    {
        if (rl != null)
        {
            for (Regex regex : rl)
            {
                if (regex.matches(str))
                    return regex;
            }
        }
        return null;
    }

    public boolean matchesAll(List<String> strs, String type)
    {
        List<Regex> rl = regexList.get(type);
        for (String str : strs)
        {
            Regex r = matches(rl, str);
            if (r == null)
                return false;
        }
        return true;
    }

    public boolean matchesAll(List<String> strs)
    {
        boolean f;
        for (String str : strs)
        {
            f = false;
            for (List<Regex> rl : regexList.values())
            {
                Regex r = matches(rl, str);
                if (r != null)
                {
                    f = true;
                    break;
                }
            }
            if (!f)
                return false;
        }
        return true;
    }

    public class Regex
    {
        private Pattern regex;
        private int region;
        private String type;

        public Regex(String regex, int region, String type, boolean ignoreCase)
        {
            this.regex = Pattern.compile(regex, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
            this.region = region;
            this.type = type;
        }

        public int getRegion()
        {
            return region;
        }

        public String getType()
        {
            return type;
        }

        public boolean matches(String str)
        {
            return regex.matcher(str).matches();
        }
    }
}