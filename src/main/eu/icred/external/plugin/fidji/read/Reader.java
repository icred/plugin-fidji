package eu.icred.external.plugin.fidji.read;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import eu.icred.model.datatype.Area;
import eu.icred.model.datatype.enumeration.AccountingStandard;
import eu.icred.model.datatype.enumeration.AreaMeasurement;
import eu.icred.model.datatype.enumeration.AreaType;
import eu.icred.model.datatype.enumeration.PeriodValueType;
import eu.icred.model.datatype.enumeration.Subset;
import eu.icred.model.node.Container;
import eu.icred.model.node.Data;
import eu.icred.model.node.Meta;
import eu.icred.model.node.Period;
import eu.icred.model.node.entity.Building;
import eu.icred.model.node.entity.Company;
import eu.icred.model.node.entity.Lease;
import eu.icred.model.node.entity.LeasedUnit;
import eu.icred.model.node.entity.Property;
import eu.icred.model.node.entity.Unit;
import eu.icred.model.node.group.Address;
import eu.icred.plugin.PluginComponent;
import eu.icred.plugin.worker.WorkerConfiguration;
import eu.icred.plugin.worker.input.IImportWorker;
import eu.icred.plugin.worker.input.ImportWorkerConfiguration;

public class Reader implements IImportWorker {
    private static Logger logger = Logger.getLogger(Reader.class);

    public static final Subset[] SUPPORTED_SUBSETS = { Subset.S5_1 };
    private static String PARAMETER_NAME = "fidji-file";

    private Container container = null;
    private XMLStreamReader xmlStream = null;

    private Stack<String> nodeStack;

    @Override
    public List<Subset> getSupportedSubsets() {
        return Arrays.asList(SUPPORTED_SUBSETS);
    }

    @Override
    public void load(WorkerConfiguration config) {
        throw new RuntimeException("not allowed");
    }

    @Override
    public void unload() {
        try {
            xmlStream.close();
        } catch (Throwable t) {
        }
        xmlStream = null;
    }

    @Override
    public void load(ImportWorkerConfiguration config) {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        try {
            xmlStream = factory.createXMLStreamReader(config.getStreams().get(PARAMETER_NAME), "UTF-8");
            nodeStack = new Stack<String>();

            container = new Container();
            Meta meta = container.getMeta();
            meta.setCreator("icred with fidji plugin");
            meta.setFormat("XML");
            meta.setVersion("1-0.6.2");
            meta.setCreated(LocalDateTime.now());
            Period period = new Period();
            Data data = new Data();
            period.setData(data);

            Company currentCompany = null;
            Property currentProperty = null;
            Building currentBuilding = null;
            Address currentAddress = null;
            Unit currentUnit = null;
            Lease currentLease = null;

            List<Property> currentProperties = new ArrayList<Property>();

            Currency mainCurrency = null;
            AreaMeasurement mainAreaMeasurement = null;

            while (xmlStream.hasNext()) {
                xmlStream.next();
                if (xmlStream.isStartElement()) {
                    String name = xmlStream.getLocalName();
                    nodeStack.push(name);

                    String xPath = nodeStack.toString().substring(1).replaceAll("\\]$", "").replaceAll(", ", "/");

                    if (xPath.equals("FIDJI")) {
                        String situation = getAttributeByName("situation");
                        LocalDate toDate = LocalDate.parse(situation);
                        LocalDate fromDate = toDate.withDayOfMonth(1);
                        period.setTo(toDate);
                        period.setFrom(fromDate);
                        period.setPeriodType(org.joda.time.Period.months(1));
                        period.setIdentifier(toDate.getYear() + "-" + toDate.getMonthOfYear());
                        period.setValueType(PeriodValueType.OTHER);

                        // Property:
                    } else if (xPath.equals("FIDJI/ASTl/AST00")) {
                        String AST00_id = getAttributeByName("id");
                        String AST00_name = getAttributeByName("name");

                        currentAddress = new Address();

                        currentProperty = new Property();
                        currentProperty.setObjectIdSender(AST00_id);
                        currentProperty.setLabel(AST00_name);
                        currentProperty.setAddress(currentAddress);
                        currentProperties.add(currentProperty);

                        currentBuilding = new Building();
                        currentBuilding.setObjectIdSender(AST00_id);
                        currentBuilding.setLabel(AST00_name);
                        currentBuilding.setAddress(currentAddress);
                        Map<String, Building> buildings = new HashMap<String, Building>();
                        buildings.put(AST00_id, currentBuilding);
                        currentProperty.setBuildings(buildings);

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST70")) {
                        String AST70 = xmlStream.getElementText();

                        currentProperty.setObjectIdReceiver(AST70);
                        currentBuilding.setObjectIdReceiver(AST70);

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST22gADl/gAD00/gAD01")) {
                        currentAddress.setStreet(xmlStream.getElementText());

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST22gADl/gAD00/gAD04")) {
                        currentAddress.setZip(xmlStream.getElementText());

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST22gADl/gAD00/gAD05")) {
                        currentAddress.setCity(xmlStream.getElementText());

                        // Property-End

                        // Unit
                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST24PRTl/PRT00") || xPath.equals("FIDJI/ASTl/AST00/AST23PRTl/PRT00")) {
                        String id = getAttributeByName("id");

                        currentUnit = new Unit();
                        currentUnit.setObjectIdSender(id);
                        currentUnit.setAreaMeasurement(AreaMeasurement.SQM);
                        currentUnit.setHash(getUnitHash("building", currentBuilding.getObjectIdSender(), id));

                        Map<String, Unit> units = currentBuilding.getUnits();
                        if (units == null) {
                            units = new HashMap<String, Unit>();
                            currentBuilding.setUnits(units);
                        }
                        units.put(id, currentUnit);

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST24PRTl/PRT00/PRT25") || xPath.equals("FIDJI/ASTl/AST00/AST23PRTl/PRT00/PRT25")) {
                        currentUnit.setObjectIdReceiver(xmlStream.getElementText());

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST24PRTl/PRT00/PRT24") || xPath.equals("FIDJI/ASTl/AST00/AST23PRTl/PRT00/PRT24")) {
                        String PRT24 = xmlStream.getElementText();
                        currentUnit.setNumberOfRooms(Double.parseDouble(PRT24));

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST24PRTl/PRT00/PRT18") || xPath.equals("FIDJI/ASTl/AST00/AST23PRTl/PRT00/PRT18")) {
                        Address unitAddress = currentUnit.getAddress();
                        if (unitAddress == null) {
                            unitAddress = new Address();
                            currentUnit.setAddress(unitAddress);
                        }
                        unitAddress.setFloor(xmlStream.getElementText());

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST24PRTl/PRT00/PRT21") || xPath.equals("FIDJI/ASTl/AST00/AST23PRTl/PRT00/PRT21")) {
                        currentUnit.setLettableUnits(new Double(Double.parseDouble(xmlStream.getElementText())).intValue());

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST24PRTl/PRT00/PRT05") || xPath.equals("FIDJI/ASTl/AST00/AST23PRTl/PRT00/PRT05")) {
                        currentUnit.setLettableArea(new Area(Double.parseDouble(xmlStream.getElementText()), AreaMeasurement.SQM, AreaType.NOT_SPECIFIED));

                        // Unit-End

                        // Lease
                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST25LEAl/LEA00")) {
                        String id = getAttributeByName("id");

                        currentLease = new Lease();
                        currentLease.setObjectIdSender(id);

                        Map<String, Lease> leases = currentProperty.getLeases();
                        if (leases == null) {
                            leases = new HashMap<String, Lease>();
                            currentProperty.setLeases(leases);
                        }
                        leases.put(id, currentLease);

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST25LEAl/LEA00/LEA38")) {
                        currentLease.setBeginRentPayment(LocalDate.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST25LEAl/LEA00/LEA05")) {
                        currentLease.setContractCompletionDate(LocalDate.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST25LEAl/LEA00/LEA07")) {
                        currentLease.setDmiendOption(LocalDate.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("FIDJI/ASTl/AST00/AST25LEAl/LEA00/LEA22aLPl/aLP00")) {
                        String leasedUnitId = getAttributeByName("idPRT");

                        LeasedUnit lUnit = new LeasedUnit();
                        String hash = getUnitHash("building", currentBuilding.getObjectIdSender(), leasedUnitId);
                        lUnit.setHash(hash);

                        Map<String, LeasedUnit> units = currentLease.getLeasedUnits();
                        if (units == null) {
                            units = new HashMap<String, LeasedUnit>();
                            currentLease.setLeasedUnits(units);
                        }
                        units.put(hash, lUnit);

                        // Lease-End

                        // Company:
                    } else if (xPath.equals("FIDJI/gHOl/gHO00")) {
                        String gHOl_name = getAttributeByName("name");
                        String gHOl_id = getAttributeByName("id");

                        currentCompany = new Company();
                        currentCompany.setObjectIdSender(gHOl_id);
                        currentCompany.setLabel(gHOl_name);
                        data.getCompanies().put(gHOl_id, currentCompany);

                    } else if (xPath.equals("FIDJI/gHOl/gHO00/gHO02")) {
                        String AST_id = getAttributeByName("idRef-AST");
                        Map<String, Property> properties = currentCompany.getProperties();
                        if (properties == null) {
                            properties = new HashMap<String, Property>();
                            currentCompany.setProperties(properties);
                        }
                        properties.put(AST_id, null);
                        // Company-End
                    }
                }

                if (xmlStream.isEndElement()) {
                    nodeStack.pop();
                }
            }

            for (Property property : currentProperties) {
                Map<String, Company> companies = data.getCompanies();
                for (Company company : companies.values()) {
                    Map<String, Property> properties = company.getProperties();
                    if (properties != null) {
                        String propId = property.getObjectIdSender();

                        if (properties.containsKey(propId)) {
                            properties.put(propId, property);
                            break;
                        }
                    }
                }
            }
            container.getPeriods().put(period.getIdentifier(), period);

            // data.getCompanies().put(com.getObjectIdSender(), com);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String getUnitHash(String parentType, String parentId, String unitId) {
        String result = parentType + "-" + parentId + "-" + unitId;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(parentType.getBytes());
            md.update(parentId.getBytes());
            md.update(unitId.getBytes());

            result = new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
        }

        return result;
    }

    private String getAttributeByName(String name) {
        for (int i = 0; i < xmlStream.getAttributeCount(); i++) {
            if (xmlStream.getAttributeLocalName(i).equals(name)) {
                return xmlStream.getAttributeValue(i);
            }
        }

        return null;
    }

    @Override
    public ImportWorkerConfiguration getRequiredConfigurationArguments() {
        return new ImportWorkerConfiguration() {
            {
                SortedMap<String, InputStream> streams = getStreams();
                streams.put(PARAMETER_NAME, null);
            }
        };
    }

    @Override
    public PluginComponent<ImportWorkerConfiguration> getConfigGui() {
        // null => DefaultConfigGui
        return null;
    }

    @Override
    public Container getContainer() {
        return container;
    }
}
