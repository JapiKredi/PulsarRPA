package fun.platonic.pulsar.persist;

import fun.platonic.pulsar.persist.metadata.Mark;
import org.apache.avro.util.Utf8;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Collectors;

import static fun.platonic.pulsar.persist.WebPage.wrapKey;

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class CrawlMarks {

    private Map<CharSequence, CharSequence> marks;

    private CrawlMarks(Map<CharSequence, CharSequence> marks) {
        this.marks = marks;
    }

    @Nonnull
    public static CrawlMarks box(Map<CharSequence, CharSequence> marks) {
        return new CrawlMarks(marks);
    }

    public Map<CharSequence, CharSequence> unbox() {
        return marks;
    }

    public Utf8 get(Mark mark) {
        return (Utf8) marks.get(wrapKey(mark));
    }

    public boolean contains(Mark mark) {
        return get(mark) != null;
    }

    public boolean isInactive() {
        return contains(Mark.INACTIVE) || contains(Mark.SEMI_INACTIVE);
    }

    public void put(Mark mark, String value) {
        put(mark, WebPage.u8(value));
    }

    public void put(Mark mark, Utf8 value) {
        marks.put(wrapKey(mark), value);
    }

    public void putIfNonNull(Mark mark, Utf8 value) {
        if (value != null) {
            put(mark, value);
        }
    }

    public void remove(Mark mark) {
        if (contains(mark)) {
            marks.put(wrapKey(mark), null);
        }
    }

    public Map<String, String> asStringMap() {
        return marks.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString(), (e, e2) -> e));
    }

    @Override
    public String toString() {
        return marks.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
    }
}
