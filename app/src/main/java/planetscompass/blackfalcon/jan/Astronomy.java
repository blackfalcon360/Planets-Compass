package planetscompass.blackfalcon.jan;

/**
 * Offline sky maths: positions of the Sun, Moon and planets for any time and place.
 * Based on Paul Schlyter's low-precision method (about 1-2 arc-minutes; fine for a sky map).
 * Azimuth is measured from TRUE north, clockwise (N=0, E=90, S=180, W=270).
 */
public final class Astronomy {

    public static final String[] NAMES = {
            "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"};

    public static final class Body {
        public final String name;
        public double ra;        // degrees
        public double dec;       // degrees
        public double azimuth;   // degrees from true north
        public double altitude;  // degrees above the horizon (negative = below)
        public double distance;  // AU (Moon: Earth radii)

        Body(String name) {
            this.name = name;
        }
    }

    // N, i, w, a, e, M : value and change per day (planets Mercury..Neptune)
    private static final double[][] EL = {
            {48.3313, 3.24587E-5, 7.0047, 5.00E-8, 29.1241, 1.01444E-5, 0.387098, 0, 0.205635, 5.59E-10, 168.6562, 4.0923344368},
            {76.6799, 2.46590E-5, 3.3946, 2.75E-8, 54.8910, 1.38374E-5, 0.723330, 0, 0.006773, -1.302E-9, 48.0052, 1.6021302244},
            {49.5574, 2.11081E-5, 1.8497, -1.78E-8, 286.5016, 2.92961E-5, 1.523688, 0, 0.093405, 2.516E-9, 18.6021, 0.5240207766},
            {100.4542, 2.76854E-5, 1.3030, -1.557E-7, 273.8777, 1.64505E-5, 5.20256, 0, 0.048498, 4.469E-9, 19.8950, 0.0830853001},
            {113.6634, 2.38980E-5, 2.4886, -1.081E-7, 339.3939, 2.97661E-5, 9.55475, 0, 0.055546, -9.499E-9, 316.9670, 0.0334442282},
            {74.0005, 1.3978E-5, 0.7733, 1.9E-8, 96.6612, 3.0565E-5, 19.18171, -1.55E-8, 0.047318, 7.45E-9, 142.5905, 0.011725806},
            {131.7806, 3.0173E-5, 1.7700, -2.55E-7, 272.8461, -6.027E-6, 30.05826, 3.313E-8, 0.008606, 2.15E-9, 260.2471, 0.005995147}
    };

    private Astronomy() {
    }

    private static double rev(double x) {
        return x - 360.0 * Math.floor(x / 360.0);
    }

    private static double sind(double x) {
        return Math.sin(Math.toRadians(x));
    }

    private static double cosd(double x) {
        return Math.cos(Math.toRadians(x));
    }

    private static double kepler(double m, double e) {
        double ea = m + Math.toDegrees(e * sind(m) * (1 + e * cosd(m)));
        for (int k = 0; k < 30; k++) {
            double e1 = ea - (ea - Math.toDegrees(e * sind(ea)) - m) / (1 - e * cosd(ea));
            boolean done = Math.abs(e1 - ea) < 1e-7;
            ea = e1;
            if (done) break;
        }
        return ea;
    }

    /** Sun: {ecliptic longitude, distance AU, mean anomaly, argument of perihelion}. */
    private static double[] sun(double d) {
        double w = 282.9404 + 4.70935E-5 * d;
        double e = 0.016709 - 1.151E-9 * d;
        double m = rev(356.0470 + 0.9856002585 * d);
        double ea = kepler(m, e);
        double xv = cosd(ea) - e;
        double yv = Math.sqrt(1 - e * e) * sind(ea);
        double v = Math.toDegrees(Math.atan2(yv, xv));
        double r = Math.hypot(xv, yv);
        return new double[]{rev(v + w), r, m, w};
    }

    /** Geocentric ecliptic rectangular coordinates of a planet (index 0 = Mercury ... 6 = Neptune). */
    private static double[] planet(int idx, double d) {
        double[] t = EL[idx];
        double n = t[0] + t[1] * d;
        double inc = t[2] + t[3] * d;
        double w = t[4] + t[5] * d;
        double a = t[6] + t[7] * d;
        double e = t[8] + t[9] * d;
        double m = rev(t[10] + t[11] * d);

        double ea = kepler(m, e);
        double xv = a * (cosd(ea) - e);
        double yv = a * Math.sqrt(1 - e * e) * sind(ea);
        double v = Math.toDegrees(Math.atan2(yv, xv));
        double r = Math.hypot(xv, yv);

        double xh = r * (cosd(n) * cosd(v + w) - sind(n) * sind(v + w) * cosd(inc));
        double yh = r * (sind(n) * cosd(v + w) + cosd(n) * sind(v + w) * cosd(inc));
        double zh = r * sind(v + w) * sind(inc);
        double lon = Math.toDegrees(Math.atan2(yh, xh));
        double lat = Math.toDegrees(Math.atan2(zh, Math.hypot(xh, yh)));

        // mutual perturbations of the big planets
        double mj = rev(19.8950 + 0.0830853001 * d);
        double ms = rev(316.9670 + 0.0334442282 * d);
        double mu = rev(142.5905 + 0.011725806 * d);
        if (idx == 3) { // Jupiter
            lon += -0.332 * sind(2 * mj - 5 * ms - 67.6) - 0.056 * sind(2 * mj - 2 * ms + 21)
                    + 0.042 * sind(3 * mj - 5 * ms + 21) - 0.036 * sind(mj - 2 * ms)
                    + 0.022 * cosd(mj - ms) + 0.023 * sind(2 * mj - 3 * ms + 52)
                    - 0.016 * sind(mj - 5 * ms - 69);
        } else if (idx == 4) { // Saturn
            lon += 0.812 * sind(2 * mj - 5 * ms - 67.6) - 0.229 * cosd(2 * mj - 4 * ms - 2)
                    + 0.119 * sind(mj - 2 * ms - 3) + 0.046 * sind(2 * mj - 6 * ms - 69)
                    + 0.014 * sind(mj - 3 * ms + 32);
            lat += -0.020 * cosd(2 * mj - 4 * ms - 2) + 0.018 * sind(2 * mj - 6 * ms - 49);
        } else if (idx == 5) { // Uranus
            lon += 0.040 * sind(ms - 2 * mu + 6) + 0.035 * sind(ms - 3 * mu + 33)
                    - 0.015 * sind(mj - mu + 20);
        }

        xh = r * cosd(lon) * cosd(lat);
        yh = r * sind(lon) * cosd(lat);
        zh = r * sind(lat);

        double[] s = sun(d);
        double xs = s[1] * cosd(s[0]);
        double ys = s[1] * sind(s[0]);
        return new double[]{xh + xs, yh + ys, zh};
    }

    /** Moon: geocentric ecliptic rectangular coordinates in Earth radii. */
    private static double[] moon(double d) {
        double n = rev(125.1228 - 0.0529538083 * d);
        double inc = 5.1454;
        double w = rev(318.0634 + 0.1643573223 * d);
        double a = 60.2666;
        double e = 0.054900;
        double mm = rev(115.3654 + 13.0649929509 * d);

        double ea = kepler(mm, e);
        double xv = a * (cosd(ea) - e);
        double yv = a * Math.sqrt(1 - e * e) * sind(ea);
        double v = Math.toDegrees(Math.atan2(yv, xv));
        double r = Math.hypot(xv, yv);

        double xh = r * (cosd(n) * cosd(v + w) - sind(n) * sind(v + w) * cosd(inc));
        double yh = r * (sind(n) * cosd(v + w) + cosd(n) * sind(v + w) * cosd(inc));
        double zh = r * sind(v + w) * sind(inc);
        double lon = Math.toDegrees(Math.atan2(yh, xh));
        double lat = Math.toDegrees(Math.atan2(zh, Math.hypot(xh, yh)));

        double[] s = sun(d);
        double ms = s[2];
        double ls = rev(s[2] + s[3]);
        double lm = rev(mm + w + n);
        double dd = rev(lm - ls);
        double f = rev(lm - n);

        lon += -1.274 * sind(mm - 2 * dd) + 0.658 * sind(2 * dd) - 0.186 * sind(ms)
                - 0.059 * sind(2 * mm - 2 * dd) - 0.057 * sind(mm - 2 * dd + ms)
                + 0.053 * sind(mm + 2 * dd) + 0.046 * sind(2 * dd - ms) + 0.041 * sind(mm - ms)
                - 0.035 * sind(dd) - 0.031 * sind(mm + ms) - 0.015 * sind(2 * f - 2 * dd)
                + 0.011 * sind(mm - 4 * dd);
        lat += -0.173 * sind(f - 2 * dd) - 0.055 * sind(mm - f - 2 * dd)
                - 0.046 * sind(mm + f - 2 * dd) + 0.033 * sind(f + 2 * dd) + 0.017 * sind(2 * mm + f);
        r += -0.58 * cosd(mm - 2 * dd) - 0.46 * cosd(2 * dd);

        return new double[]{r * cosd(lon) * cosd(lat), r * sind(lon) * cosd(lat), r * sind(lat), r};
    }

    /**
     * Positions of the Sun, Moon and the seven planets.
     *
     * @param utcMillis time as milliseconds since 1970 (UTC)
     * @param latDeg    observer latitude, north positive
     * @param lonDeg    observer longitude, east positive
     */
    public static Body[] compute(long utcMillis, double latDeg, double lonDeg) {
        double jd = utcMillis / 86400000.0 + 2440587.5;
        double d = jd - 2451543.5;
        double ecl = 23.4393 - 3.563E-7 * d;

        double gmst = rev(280.46061837 + 360.98564736629 * (jd - 2451545.0));
        double lst = rev(gmst + lonDeg);

        Body[] out = new Body[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            double xg, yg, zg, dist;
            if (i == 0) {
                double[] s = sun(d);
                xg = s[1] * cosd(s[0]);
                yg = s[1] * sind(s[0]);
                zg = 0;
                dist = s[1];
            } else if (i == 1) {
                double[] m = moon(d);
                xg = m[0];
                yg = m[1];
                zg = m[2];
                dist = m[3];
            } else {
                double[] p = planet(i - 2, d);
                xg = p[0];
                yg = p[1];
                zg = p[2];
                dist = Math.sqrt(xg * xg + yg * yg + zg * zg);
            }

            double xe = xg;
            double ye = yg * cosd(ecl) - zg * sind(ecl);
            double ze = yg * sind(ecl) + zg * cosd(ecl);
            double ra = rev(Math.toDegrees(Math.atan2(ye, xe)));
            double dec = Math.toDegrees(Math.atan2(ze, Math.hypot(xe, ye)));

            double ha = Math.toRadians(rev(lst - ra));
            double la = Math.toRadians(latDeg);
            double de = Math.toRadians(dec);
            double alt = Math.toDegrees(Math.asin(
                    Math.sin(de) * Math.sin(la) + Math.cos(de) * Math.cos(la) * Math.cos(ha)));
            double az = rev(Math.toDegrees(Math.atan2(Math.sin(ha),
                    Math.cos(ha) * Math.sin(la) - Math.tan(de) * Math.cos(la))) + 180.0);

            if (i == 1) { // the Moon is close: correct for parallax
                double parallax = Math.toDegrees(Math.asin(1.0 / dist));
                alt -= parallax * Math.cos(Math.toRadians(alt));
            }

            Body b = new Body(NAMES[i]);
            b.ra = ra;
            b.dec = dec;
            b.azimuth = az;
            b.altitude = alt;
            b.distance = dist;
            out[i] = b;
        }
        return out;
    }
}
