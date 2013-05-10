package com.typemapper.namedresult.results;

import com.google.common.base.Optional;

import com.typemapper.annotations.DatabaseField;
import com.typemapper.annotations.DatabaseType;

@DatabaseType(name = "additional_type")
public class ClassWithOptionalPrimitives {

    @DatabaseField(name = "i")
    public Optional<Integer> i;

    @DatabaseField(name = "l")
    public Optional<Long> l;

    @DatabaseField(name = "c")
    public Optional<Character> c;

    public ClassWithOptionalPrimitives() { }

    public ClassWithOptionalPrimitives(final int i, final long l, final char c) {
        this.i = Optional.of(i);
        this.l = Optional.of(l);
        this.c = Optional.of(c);
    }

    public Optional<Integer> getI() {
        return i;
    }

    public void setI(final Optional<Integer> i) {
        this.i = i;
    }

    public Optional<Long> getL() {
        return l;
    }

    public void setL(final Optional<Long> l) {
        this.l = l;
    }

    public Optional<Character> getC() {
        return c;
    }

    public void setC(final Optional<Character> c) {
        this.c = c;
    }
}
