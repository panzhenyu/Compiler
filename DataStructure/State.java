package DataStructure;

import java.util.HashSet;

public class State implements Cloneable, Comparable<State>
{
    Integer s;
    boolean isEndingState;

    public State(Integer s, boolean isEndingState)
    {
        this.s = s;
        this.isEndingState = isEndingState;
    }

    public boolean isEmptyState()
    {
        return this.s == null;
    }

    public void setS(Integer news)
    {
        this.s = news;
    }

    public Integer getS()
    {
        return s;
    }

    public String toString()
    {
        return "" + s;
    }

    public State clone()
    {
        State o = null;
        try {
            o = (State)super.clone();
        } catch (CloneNotSupportedException e) {}
        return o;
    }

    public boolean equals(Object o)
    {
        if(!(o instanceof State))
            return false;
        State s = (State)o;
        return this.s.equals(s.s) && this.isEndingState == s.isEndingState;
    }

    public int compareTo(State o)
    {
        return this.s - o.s;
    }

    public static void main(String[] args) throws Exception
    {
        HashSet<State> hs = new HashSet<>();
        hs.add(new State(1, false));
        System.out.println(hs.contains(new State(1,false)));
    }
}
