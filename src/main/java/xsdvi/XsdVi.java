package xsdvi;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import xsdvi.svg.AbstractSymbol;
import xsdvi.svg.SvgForXsd;
import xsdvi.utils.LoggerHelper;
import xsdvi.utils.TreeBuilder;
import xsdvi.utils.WriterHelper;
import xsdvi.utils.XsdErrorHandler;

/**
 * @author Václav Slavìtínský
 *
 */
public final class XsdVi {

    private static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private static final List<String> inputs = new ArrayList<>();
    private static String style = null;
    private static String styleUrl = null;
    private static String rootNodeName = null;
    private static List<Short> rootTypes = null;
    private static final Map<String, Short> allowedRootTypes = Map.of(
            "element", XSConstants.ELEMENT_DECLARATION,
            "type", XSConstants.TYPE_DEFINITION);
    private static boolean oneNodeOnly = false;
    private static String outputPath = null;

    // Options
    public static final String ROOT_NODE_NAME = "rootNodeName";
    public static final String ONE_NODE_ONLY = "oneNodeOnly";
    public static final String OUTPUT_PATH = "outputPath";
    public static final String EMBODY_STYLE = "embodyStyle";
    public static final String GENERATE_STYLE = "generateStyle";
    public static final String USE_STYLE = "useStyle";
    public static final String ROOT_TYPES = "rootTypes";

    static final Option optionRootNodeName = Option.builder(ROOT_NODE_NAME)
            .desc(" schema root node name (or 'all' for all elements)")
            .hasArg()
            .required(false)
            .build();

    static final Option optionOneNodeOnly = Option.builder(ONE_NODE_ONLY)
            .desc(" show only one element")
            .required(false)
            .build();

    static final Option optionOutputPath = Option.builder(OUTPUT_PATH)
            .desc(" output folder")
            .hasArg()
            .required(false)
            .build();

    static final Option optionEmbodyStyle = Option.builder(EMBODY_STYLE)
            .desc(" css style will be embodied in each svg file, this is default")
            .required(true)
            .build();

    static final Option optionGenerateStyle = Option.builder(GENERATE_STYLE)
            .desc(" new css file with specified name will be generated and used by svgs")
            .hasArg()
            .required(true)
            .build();

    static final Option optionUseStyle = Option.builder(USE_STYLE)
            .desc(" external css file at specified url will be used by svgs")
            .hasArg()
            .required(true)
            .build();

    static final Option optionRootTypes = Option.builder(ROOT_TYPES)
            .desc(" specify which XSD types to use as potential root nodes")
            .hasArg()
            .required(true)
            .build();

    static final Options options = new Options() {
        private static final long serialVersionUID = -6797031893776761837L;

        {
            addOption(optionRootNodeName);
            addOption(optionOneNodeOnly);
            addOption(optionOutputPath);
            addOption(optionRootTypes);
        }
    };

    static final Options optionsEmbodyStyle = new Options() {
        private static final long serialVersionUID = 702296838422916825L;

        {
            addOption(optionRootNodeName);
            addOption(optionOneNodeOnly);
            addOption(optionOutputPath);
            addOption(optionEmbodyStyle);
            addOption(optionRootTypes);
        }
    };

    static final Options optionsGenerateStyle = new Options() {
        private static final long serialVersionUID = -7868166441913860186L;

        {
            addOption(optionRootNodeName);
            addOption(optionOneNodeOnly);
            addOption(optionOutputPath);
            addOption(optionGenerateStyle);
            addOption(optionRootTypes);
        }
    };

    static final Options optionsUseStyle = new Options() {
        private static final long serialVersionUID = -2296504645200494193L;

        {
            addOption(optionRootNodeName);
            addOption(optionOneNodeOnly);
            addOption(optionOutputPath);
            addOption(optionUseStyle);
            addOption(optionRootTypes);
        }
    };

    static final String CMD_COMMON_PREFIX = "java -jar xsdvi.jar <input1.xsd> [<input2.xsd> [<input3.xsd> ...]]";
    static final String CMD_COMMON_SUFFIX = " [-" + ROOT_NODE_NAME + " <name>] [-" + ONE_NODE_ONLY + "] [-" + OUTPUT_PATH + " <arg>]";
    static final String CMD = CMD_COMMON_PREFIX + CMD_COMMON_SUFFIX;
    static final String CMD_EMBODY_STYLE = CMD_COMMON_PREFIX + " [-" + EMBODY_STYLE + "] " + CMD_COMMON_SUFFIX;
    static final String CMD_GENERATE_STYLE = CMD_COMMON_PREFIX + " [-" + GENERATE_STYLE + " <arg>] " + CMD_COMMON_SUFFIX;
    static final String CMD_USE_STYLE = CMD_COMMON_PREFIX + " [-" + USE_STYLE + " <arg>] " + CMD_COMMON_SUFFIX;

    static final String INPUT_NOT_FOUND = "Error: %s file '%s' not found!";
    static final String XSD_INPUT = "XSD";
    static final String USAGE = getUsage();
    static final int ERROR_EXIT_CODE = -1;

    private XsdVi() {
        // no instances
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        LoggerHelper.setupLogger();
        inputs.clear();

        parseArgs(args);

        XSLoader schemaLoader = getSchemaLoader();

        TreeBuilder builder = new TreeBuilder();
        XsdHandler xsdHandler = new XsdHandler(builder);
        WriterHelper writerHelper = new WriterHelper();
        SvgForXsd svg = new SvgForXsd(writerHelper);
        svg.setHideMenuButtons(oneNodeOnly);

        if (style.equals(EMBODY_STYLE)) {
            logger.info("The style will be embodied");
            svg.setEmbodyStyle(true);
        } else {
            logger.log(Level.INFO, "Using external style {0}", styleUrl);
            svg.setEmbodyStyle(false);
            svg.setStyleUri(styleUrl);
        }
        if (style.equals(GENERATE_STYLE)) {
            logger.log(Level.INFO, "Generating style {0}...", styleUrl);
            svg.printExternStyle();
            logger.info("Done.");
        }

        // check input file exists
        for (String input : inputs) {
            File fXMLin = new File(input);
            if (!fXMLin.exists()) {
                System.out.println(String.format(INPUT_NOT_FOUND, XSD_INPUT, fXMLin));
                System.exit(ERROR_EXIT_CODE);
            }
        }

        for (String input : inputs) {
            logger.log(Level.INFO, "Parsing {0}...", input);
            XSModel model = schemaLoader.loadURI(input);

            logger.info("Processing XML Schema model...");

            List<String> rootNames = new ArrayList<>();
            if (rootNodeName == null) {
                String output = outputUrl(input);
                xsdHandler.processModel(model);
                logger.log(Level.INFO, "Drawing SVG {0}...", output);
                writerHelper.newWriter(output);
                if (builder.getRoot() != null) {
                    svg.draw((AbstractSymbol) builder.getRoot());
                    logger.info("Done.");
                } else {
                    logger.severe("SVG is empty!");
                }
            } else { // rootNodeName != null
                if (rootNodeName.equals("all")) {
                    rootNames = xsdHandler.getRootNodeNames(model, rootTypes);
                } else {
                    rootNames.add(rootNodeName);
                }
                xsdHandler.setSchemaNamespace(model, rootNames.get(0));

                for (String elementName : rootNames) {
                    rootNodeName = elementName;
                    String output = outputUrl(input);
                    xsdHandler.setRootNodeName(rootNodeName);
                    xsdHandler.setOneNodeOnly(oneNodeOnly);
                    xsdHandler.processModel(model);
                    logger.log(Level.INFO, "Drawing SVG {0}...", output);
                    writerHelper.newWriter(output);
                    if (builder.getRoot() != null) {
                        svg.draw((AbstractSymbol) builder.getRoot());
                        logger.info("Done.");
                    } else {
                        logger.severe("SVG is empty!");
                    }
                }
            }
        }
        //new xsdvi.svg.SvgSymbols(writerHelper).drawSymbols();
        //logger.info("Symbols saved.");
    }

    /**
     * @param input
     * @return
     */
    private static String outputUrl(String input) {
        String[] field = input.split("[/\\\\]");
        String in = field[field.length - 1];
        String filename = ".svg";
        if (rootNodeName == null || (rootNodeName != null && oneNodeOnly == false)) {
            if (in.toLowerCase().endsWith(".xsd")) {
                filename = in.substring(0, in.length() - 4) + filename;
            } else {
                filename = in + filename;
            }
        } else {
            filename = rootNodeName + filename;
        }

        String path = "";
        if (outputPath != null) {
            path = outputPath;
            try {
                Files.createDirectories(Paths.get(path, ""));
            } catch (IOException ex) {
            }
        }
        Path localOutputPath = Paths.get(path, filename);
        return localOutputPath.toString();
    }

    /**
     * @param args
     */
    private static void parseArgs(String[] args) {

        CommandLineParser parser = new DefaultParser();

        boolean cmdFail = false;

        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            style = EMBODY_STYLE;

        } catch (ParseException exp) {
            cmdFail = true;
        }

        if (cmdFail) {
            try {
                cmd = parser.parse(optionsEmbodyStyle, args);
                style = EMBODY_STYLE;

            } catch (ParseException exp) {
                cmdFail = true;
            }
        }

        if (cmdFail) {
            try {
                cmd = parser.parse(optionsGenerateStyle, args);
                style = GENERATE_STYLE;
                styleUrl = cmd.getOptionValue(GENERATE_STYLE);

            } catch (ParseException exp) {
                cmdFail = true;
            }
        }

        if (cmdFail) {
            try {
                cmd = parser.parse(optionsUseStyle, args);
                style = USE_STYLE;
                styleUrl = cmd.getOptionValue(USE_STYLE);

            } catch (ParseException exp) {
                cmdFail = true;
            }
        }
        assert cmd != null;

        if (!cmdFail) {
            try {
                List<String> arglist = cmd.getArgList();
                if (arglist.isEmpty() || arglist.get(0).trim().length() == 0) {
                    throw new ParseException("");
                }

                rootNodeName = cmd.getOptionValue(ROOT_NODE_NAME);
                oneNodeOnly = cmd.hasOption(ONE_NODE_ONLY);

                if (cmd.hasOption(ROOT_TYPES)) {
                    rootTypes = new ArrayList<>();
                    String[] rootTypesArray = cmd.getOptionValue(ROOT_TYPES).split(",");
                    for (String s : rootTypesArray) {
                        if (!allowedRootTypes.containsKey(s.toLowerCase())) {
                            throw new ParseException("Invalid root type:" + s);
                        }
                        rootTypes.add(allowedRootTypes.get(s.toLowerCase()));
                    }
                } else {
                    rootTypes = List.of(XSConstants.ELEMENT_DECLARATION);
                }

                if (rootNodeName != null && rootNodeName.equals("all")) {
                    oneNodeOnly = true;
                }

                outputPath = cmd.getOptionValue(OUTPUT_PATH);

                inputs.addAll(cmd.getArgList());

                return;
            } catch (ParseException exp) {
                cmdFail = true;
            }
        }

        if (cmdFail) {
            printUsage();
            System.exit(ERROR_EXIT_CODE);
        }

    }

    /**
     *
     */
    private static void printUsage() {
        logger.severe(USAGE);
    }

    /**
     * @return
     */
    private static XSLoader getSchemaLoader() {
        XSLoader schemaLoader = null;
        try {
            System.setProperty(DOMImplementationRegistry.PROPERTY, "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            XSImplementation impl = (XSImplementation) registry.getDOMImplementation("XS-Loader");
            schemaLoader = impl.createXSLoader(null);
            DOMConfiguration config = schemaLoader.getConfig();
            DOMErrorHandler errorHandler = new XsdErrorHandler();
            config.setParameter("error-handler", errorHandler);
            config.setParameter("validate", Boolean.TRUE);
        } catch (ClassCastException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return schemaLoader;
    }

    private static String getUsage() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(stringWriter);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, 100, CMD, "", options, 0, 0, "");
        pw.write("\nOR\n\n");
        formatter.printHelp(pw, 100, CMD_EMBODY_STYLE, "", optionsEmbodyStyle, 0, 0, "");
        pw.write("\nOR\n\n");
        formatter.printHelp(pw, 100, CMD_GENERATE_STYLE, "", optionsGenerateStyle, 0, 0, "");
        pw.write("\nOR\n\n");
        formatter.printHelp(pw, 100, CMD_USE_STYLE, "", optionsUseStyle, 0, 0, "");
        pw.flush();
        return stringWriter.toString();
    }

}
