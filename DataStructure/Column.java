package DataStructure;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Column<T>
{
    String cname;
    ArrayList<T> cvalue;

    public Column(String cname)
    {
        this.cname = cname;
        this.cvalue = new ArrayList<>();
    }

    public boolean add(T e)
    {
        return cvalue.add(e);
    }

    public boolean addAll(Column<T> c)
    {
        return this.cvalue.addAll(c.cvalue);
    }

    public boolean addAll(ArrayList<T> cvalue)
    {
        return this.cvalue.addAll(cvalue);
    }

    public boolean remove(T o)
    {
        return this.cvalue.remove(o);
    }

    public T remove(int index)
    {
        return this.cvalue.remove(index);
    }

    public String getCname()
    {
        return this.cname;
    }

    public void rename(String newName)
    {
        this.cname = newName;
    }

    public int size()
    {
        return this.cvalue.size();
    }

    public Column<T> copy() {
        Column<T> newcol = new Column<>(this.cname);
        newcol.cvalue.addAll(this.cvalue);
        return newcol;
    }
}
