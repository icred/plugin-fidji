package eu.icred.external.plugin.fidji.write;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;

import eu.icred.external.plugin.fidji.read.Reader;
import eu.icred.model.datatype.enumeration.Subset;
import eu.icred.model.node.AbstractNode;
import eu.icred.model.node.Container;
import eu.icred.model.node.Data;
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
import eu.icred.plugin.worker.output.ExportWorkerConfiguration;
import eu.icred.plugin.worker.output.IExportWorker;

public class Writer implements IExportWorker {
    private static Logger logger = Logger.getLogger(Reader.class);

    public static final Subset[] SUPPORTED_SUBSETS = { Subset.S5_1 };
    private static String PARAMETER_NAME = "fidji-file";

    private XMLStreamWriter xmlStream = null;

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
    public ExportWorkerConfiguration getRequiredConfigurationArguments() {
        return new ExportWorkerConfiguration() {
            {
                SortedMap<String, OutputStream> streams = getStreams();
                streams.put(PARAMETER_NAME, null);
            }
        };
    }

    @Override
    public void load(ExportWorkerConfiguration config, Container container) {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        int deep = 0;
        try {
            xmlStream = factory.createXMLStreamWriter(config.getStreams().get(PARAMETER_NAME), "UTF-8");

            Period period = container.getPeriods().values().iterator().next();
            Data data = period.getData();
            Collection<Company> companies = data.getCompanies().values();

            xmlStream.writeStartDocument("utf-8", "1.0");
            xmlStream.writeCharacters("\r\n");
            xmlStream.writeStartElement("FIDJI");
            xmlStream.writeAttribute("version", "2.0");
            xmlStream.writeAttribute("date", LocalDate.now().toString());
            xmlStream.writeAttribute("situation", period.getTo().toString());
            xmlStream.writeAttribute("xmlns", "http://www.format-Fidji.org/XMLSchema-2.0");
            xmlStream.writeAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema-instance");
            xmlStream.writeAttribute("xs:schemaLocation",
                    "http://www.format-Fidji.org/XMLSchema-2.0 http://www.format-Fidji.org/XMLSchema-2.0/Fidji-Full-2-0.xsd");

            deep++;
            xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
            xmlStream.writeStartElement("ASTl");
            for (Company company : companies) {
                Map<String, Property> props = company.getProperties();
                if (props != null) {
                    for (Property prop : props.values()) {
                        for (Building build : prop.getBuildings().values()) {
                            deep++;
                            xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                            xmlStream.writeStartElement("AST00");
                            xmlStream.writeAttribute("id", build.getObjectIdSender());
                            if (build.getLabel() != null)
                                xmlStream.writeAttribute("name", build.getLabel());

                            if (build.getObjectIdReceiver() != null) {

                                deep++;
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                xmlStream.writeStartElement("AST70");
                                xmlStream.writeCharacters(build.getObjectIdReceiver());
                                deep--;
                                xmlStream.writeEndElement(); // AST70
                            }

                            Address addr = build.getAddress();
                            if (addr != null) {
                                deep++;
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                xmlStream.writeStartElement("AST22gADl");
                                deep++;
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                xmlStream.writeStartElement("gAD00");

                                deep++;
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                xmlStream.writeStartElement("gAD01");
                                xmlStream.writeCharacters(addr.getStreet());
                                deep--;
                                xmlStream.writeEndElement(); // gAD01

                                deep++;
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                xmlStream.writeStartElement("gAD04");
                                xmlStream.writeCharacters(addr.getZip());
                                deep--;
                                xmlStream.writeEndElement(); // gAD04

                                deep++;
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                xmlStream.writeStartElement("gAD05");
                                xmlStream.writeCharacters(addr.getCity());
                                deep--;
                                xmlStream.writeEndElement(); // gAD05

                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                deep--;
                                xmlStream.writeEndElement(); // gAD00
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                deep--;
                                xmlStream.writeEndElement(); // AST22gADl
                            }

                            Map<String, Unit> units = build.getUnits();
                            if (units != null) {
                                deep++;
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                xmlStream.writeStartElement("AST24PRTl");
                                for (Unit unit : units.values()) {
                                    deep++;
                                    xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                    xmlStream.writeStartElement("PRT00");
                                    xmlStream.writeAttribute("id", unit.getObjectIdSender());

                                    if (unit.getObjectIdReceiver() != null) {
                                        deep++;
                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        xmlStream.writeStartElement("PRT25");
                                        xmlStream.writeCharacters(unit.getObjectIdReceiver());
                                        deep--;
                                        xmlStream.writeEndElement();
                                    }
                                    if (unit.getNumberOfRooms() != null) {
                                        deep++;
                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        xmlStream.writeStartElement("PRT24");
                                        xmlStream.writeCharacters(unit.getNumberOfRooms().toString());
                                        deep--;
                                        xmlStream.writeEndElement();
                                    }
                                    Address unitAddr = unit.getAddress();
                                    if (unitAddr != null && unitAddr.getFloor() != null) {
                                        deep++;
                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        xmlStream.writeStartElement("PRT18");
                                        xmlStream.writeCharacters(unitAddr.getFloor());
                                        deep--;
                                        xmlStream.writeEndElement();
                                    }
                                    if (unit.getLettableUnits() != null) {
                                        deep++;
                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        xmlStream.writeStartElement("PRT21");
                                        xmlStream.writeCharacters(unit.getLettableUnits().toString());
                                        deep--;
                                        xmlStream.writeEndElement();
                                    }
                                    if (unit.getLettableArea() != null) {
                                        deep++;
                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        xmlStream.writeStartElement("PRT05");
                                        xmlStream.writeCharacters(unit.getLettableArea().getValue().toString());
                                        deep--;
                                        xmlStream.writeEndElement();
                                    }

                                    xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                    deep--;
                                    xmlStream.writeEndElement(); // PRT00
                                }
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                deep--;
                                xmlStream.writeEndElement(); // AST24PRTl
                            }

                            Map<String, Lease> leases = prop.getLeases();
                            if (leases != null) {
                                deep++;
                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                xmlStream.writeStartElement("AST25LEAl");

                                for (Lease lease : leases.values()) {
                                    deep++;
                                    xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                    xmlStream.writeStartElement("LEA00");
                                    xmlStream.writeAttribute("id", lease.getObjectIdSender());

                                    if (lease.getBeginRentPayment() != null) {
                                        deep++;
                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        xmlStream.writeStartElement("LEA38");
                                        xmlStream.writeCharacters(lease.getBeginRentPayment().toString());
                                        deep--;
                                        xmlStream.writeEndElement();
                                    }
                                    if (lease.getContractCompletionDate() != null) {
                                        deep++;
                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        xmlStream.writeStartElement("LEA05");
                                        xmlStream.writeCharacters(lease.getContractCompletionDate().toString());
                                        deep--;
                                        xmlStream.writeEndElement();
                                    }
                                    if (lease.getDmiendOption() != null) {
                                        deep++;
                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        xmlStream.writeStartElement("LEA07");
                                        xmlStream.writeCharacters(lease.getDmiendOption().toString());
                                        deep--;
                                        xmlStream.writeEndElement();
                                    }
                                    Map<String, LeasedUnit> leasedUnits = lease.getLeasedUnits();
                                    if (leasedUnits != null) {
                                        deep++;
                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        xmlStream.writeStartElement("LEA22aLPl");

                                        for (AbstractNode lUnit : leasedUnits.values()) {
                                            String id = "";
                                            if(lUnit instanceof Unit) {
                                                id = getUnitIdByHash(prop, ((Unit)lUnit).getHash());
                                            }if(lUnit instanceof LeasedUnit) {
                                                id = getUnitIdByHash(prop, ((LeasedUnit)lUnit).getHash());
                                            }
                                            if(id == null) {
                                                id = "";
                                            }
                                            
                                            deep++;
                                            xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                            xmlStream.writeStartElement("aLP00");
                                            xmlStream.writeAttribute("idPRT", id);
                                            deep--;
                                            xmlStream.writeEndElement();
                                        }

                                        xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                        deep--;
                                        xmlStream.writeEndElement(); // LEA22aLPl
                                    }

                                    xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                    deep--;
                                    xmlStream.writeEndElement(); // LEA00
                                }

                                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                                deep--;
                                xmlStream.writeEndElement(); // AST25LEAl
                            }

                            xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                            deep--;
                            xmlStream.writeEndElement(); // AST00
                        }
                    }
                }
            }
            xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
            deep--;
            xmlStream.writeEndElement(); // ASTl

            deep++;
            xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
            xmlStream.writeStartElement("gHOl");
            for (Company com : companies) {
                deep++;
                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                xmlStream.writeStartElement("gHO00");
                xmlStream.writeAttribute("id", com.getObjectIdSender());
                if (com.getLabel() != null)
                    xmlStream.writeAttribute("name", com.getLabel());

                Map<String, Property> props = com.getProperties();
                if (props != null) {
                    Property firstProp = props.values().iterator().next();

                    deep++;
                    xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                    xmlStream.writeStartElement("gHO02");
                    xmlStream.writeAttribute("idRef-AST", firstProp.getObjectIdSender());
                    deep--;
                    xmlStream.writeEndElement(); // gHO02
                }
                xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
                deep--;
                xmlStream.writeEndElement(); // gHO00
            }
            xmlStream.writeCharacters("\r\n" + new String(new char[deep]).replace("\0", "  "));
            deep--;
            xmlStream.writeEndElement(); // gHOl

            xmlStream.writeCharacters("\r\n");
            xmlStream.writeEndElement();
            xmlStream.writeEndDocument();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String getUnitIdByHash(Property prop, String hash) {
        Map<String, Building> builds = prop.getBuildings();
        if (builds != null) {
            for (Building build : builds.values()) {
                Map<String, Unit> units = build.getUnits();
                if(units != null) {
                    for (Unit unit : units.values()) {
                        if(unit.getHash().equals(hash)) {
                            return unit.getObjectIdSender();
                        }
                    }
                }
            }
        }
        
        return null;
    }

    @Override
    public PluginComponent<ExportWorkerConfiguration> getConfigGui() {
        // null -> DefaultGui
        return null;
    }

}
