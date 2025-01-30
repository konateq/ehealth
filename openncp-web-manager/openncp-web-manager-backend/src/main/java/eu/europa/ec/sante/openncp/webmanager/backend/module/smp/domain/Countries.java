package eu.europa.ec.sante.openncp.webmanager.backend.module.smp.domain;

/**
 * @author Inês Garganta
 */
public enum Countries {

    be("Belgique/België"),
    bg("България"),
    cz("Česká Republika"),
    dk("Danmark"),
    de("Deutschland"),
    ee("Eesti"),
    ie("Éire/Ireland"),
    el("Ελλάδα"),
    es("España"),
    fr("France"),
    hr("Hrvatska"),
    is("Ísland"),
    it("Italia"),
    cy("Κύπρος"),
    lv("Latvija"),
    lt("Lietuva"),
    lu("Luxembourg"),
    hu("Magyarország"),
    mt("Malta"),
    nl("Nederland"),
    no("Noorwegen"),
    at("Österreich"),
    pl("Polska"),
    pt("Portugal"),
    ro("România"),
    si("Slovenija"),
    sk("Slovensko"),
    fi("Suomi/Finland"),
    se("Sverige"),
    uk("United Kingdom"),
    ch("Switzerland"),
    eu("European Union");

    private static final Countries[] ALL = {be, bg, cz, dk, de, ee, ie, el, es, fr, hr, is, it, cy, lv, lt, lu, hu, mt,
            nl, no, at, pl, pt, ro, si, sk, fi, se, uk, ch, eu};

    private final String description;

    Countries(final String description) {
        this.description = description;
    }

    public static Countries[] getAll() {
        return ALL;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
