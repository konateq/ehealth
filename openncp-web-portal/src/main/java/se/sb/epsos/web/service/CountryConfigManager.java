package se.sb.epsos.web.service;

import se.sb.epsos.web.model.CountryVO;
import se.sb.epsos.web.model.PatientIdVO;
import se.sb.epsos.web.util.InternationalConfigManager;
import se.sb.epsos.web.util.MasterConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CountryConfigManager {

    private static final String CONFIG_PREFIX = "CountryConfigManager.country.";
    private static final String TEXT = ".text";
    private static final String HELP_TEXT_FOR_TEST = ".HelpTextForTest";
    private static final String IDENTIFICATION_HELP_LINK = ".IdentificationHelpLink";

    private static final InternationalConfigManager internationalSearchConfig = new InternationalConfigManager(getCountries());
    private static final String INTERNATIONALSEARCHFIELDS = "country.searchFields";

    private CountryConfigManager() {
    }

    public static List<Properties> getSearchFields(String countryCode) {
        return internationalSearchConfig.getProperties(countryCode, INTERNATIONALSEARCHFIELDS);
    }

    public static List<PatientIdVO> getPatientIdentifiers(CountryVO country) {
        List<PatientIdVO> patIds = new ArrayList<>();
        List<Properties> props = getSearchFields(country.getId());

        List<String> list = internationalSearchConfig.getList(country.getId(), "country.searchFields.id[@label]");
        int i = 0;
        for (String str : list) {
            PatientIdVO vo = new PatientIdVO();
            vo.setLabel(str);
            for (Properties prop : props) {
                if (prop.containsKey("country.searchFields.id[@domain]" + i)) {
                    vo.setDomain(prop.getProperty("country.searchFields.id[@domain]" + i));
                } else if (prop.containsKey("country.searchFields.id[@max]" + i) && !prop.getProperty("country.searchFields.id[@max]" + i).equals("-1")) {
                    vo.setMax(Integer.parseInt(prop.getProperty("country.searchFields.id[@max]" + i)));
                } else if (prop.containsKey("country.searchFields.id[@min]" + i) && !prop.getProperty("country.searchFields.id[@min]" + i).equals("-1")) {
                    vo.setMin(Integer.parseInt(prop.getProperty("country.searchFields.id[@min]" + i)));
                }
            }
            patIds.add(vo);
            i++;
        }

        List<String> listTextField = internationalSearchConfig.getList(country.getId(), "country.searchFields.textField[@label]");
        for (String str : listTextField) {
            PatientIdVO vo = new PatientIdVO();
            vo.setLabel(str);
            patIds.add(vo);
        }

        return patIds;
    }

    public static String getHomeCommunityId(String countryCode) {
        return MasterConfigManager.get(CONFIG_PREFIX + countryCode + "[@homeCommunity]");
    }

    public static String getText(CountryVO country) {
        return MasterConfigManager.get(CONFIG_PREFIX + country.getId() + TEXT);
    }

    public static String getHelpTextForTest(CountryVO country) {
        return MasterConfigManager.get(CONFIG_PREFIX + country.getId() + HELP_TEXT_FOR_TEST);
    }

    public static String getIdentificationHelpLink(CountryVO country) {
        return MasterConfigManager.get(CONFIG_PREFIX + country.getId() + IDENTIFICATION_HELP_LINK);
    }

    public static List<CountryVO> getCountries() {
        List<CountryVO> countries = new ArrayList<>();
        Properties props = MasterConfigManager.getProperties("CountryConfigManager.country");
        for (Object key : props.keySet()) {
            Pattern p = Pattern.compile("^CountryConfigManager\\.country\\.([A-Z]*)\\.text$");
            Matcher m = p.matcher(key.toString());
            if (m.find()) {
                countries.add(new CountryVO(m.group(1), null, getHomeCommunityId(m.group(1))));
            }
        }
        Collections.sort(countries);
        return countries;
    }

    static CountryVO getCountry(String homeCommunityId) {
        if (homeCommunityId == null) return null;
        List<CountryVO> countries = getCountries();
        for (CountryVO country : countries) {
            if (homeCommunityId.equals(country.getHomeCommunityId())) {
                return country;
            }
        }
        return null;
    }
}
