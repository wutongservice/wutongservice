package com.borqs.server.platform.util;


import com.borqs.server.platform.io.Writable;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.io.IOException;

public class GeoLocation implements Copyable<GeoLocation>, Writable {
    public double longitude;
    public double latitude;

    public GeoLocation() {
        this(0.0, 0.0);
    }

    public GeoLocation(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        GeoLocation other = (GeoLocation) o;

        return Double.compare(longitude, other.longitude) == 0
                && Double.compare(latitude, other.latitude) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = longitude != +0.0d ? Double.doubleToLongBits(longitude) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = latitude != +0.0d ? Double.doubleToLongBits(latitude) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s#%s", longitude, latitude);
    }

    public static GeoLocation parse(String s) {
        String s1 = StringUtils.substringBefore(s, "#");
        String s2 = StringUtils.substringAfter(s, "#");
        return new GeoLocation(Double.parseDouble(s1), Double.parseDouble(s2));
    }

    @Override
    public GeoLocation copy() {
        return new GeoLocation(longitude, latitude);
    }

    @Override
    public void write(Encoder out, boolean flush) throws IOException {
        out.writeDouble(longitude);
        out.writeDouble(latitude);
        if (flush)
            out.flush();
    }

    @Override
    public void readIn(Decoder in) throws IOException {
        longitude = in.readDouble();
        latitude = in.readDouble();
    }

    public static GeoLocation fromStringPair(String longitudeStr, String latitudeStr) {
        if (StringUtils.isBlank(longitudeStr) || StringUtils.isBlank(latitudeStr))
            return null;
        else
            return new GeoLocation(NumberUtils.toDouble(longitudeStr, 0.0), NumberUtils.toDouble(latitudeStr, 0.0));
    }
}
