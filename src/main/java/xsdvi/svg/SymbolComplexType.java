package xsdvi.svg;

import java.util.ArrayList;

import xsdvi.utils.WidthCalculator;

/**
 * @author Václav Slavìtínský
 *
 */
public class SymbolComplexType extends AbstractSymbol {

    private String name = null;
    private String namespace = null;
    private String cardinality = null;
    private boolean optional = false;
    private boolean abstr = false;
    private String substitution = null;

    /**
     * @param name
     * @param namespace
     * @param type
     * @param cardinality
     * @param nillable
     * @param abstr
     * @param substitution
     */
    public SymbolComplexType(String name, String namespace, String cardinality, boolean abstr, String substitution) {
        this();
        this.name = name;
        this.namespace = namespace;
        this.cardinality = cardinality;
        this.abstr = abstr;
        this.substitution = substitution;
    }

    /**
     *
     */
    public SymbolComplexType() {
        super();
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return
     */
    public String getCardinality() {
        return cardinality;
    }

    /**
     * @param cardinality
     */
    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
        if (cardinality != null && cardinality.startsWith("0")) {
            this.optional = true;
        }
    }

    /**
     * @return
     */
    public boolean isAbstr() {
        return abstr;
    }

    /**
     * @param abstr
     */
    public void setAbstr(boolean abstr) {
        this.abstr = abstr;
    }

    /**
     * @return
     */
    public String getSubstitution() {
        return substitution;
    }

    /**
     * @param substitution
     */
    public void setSubstitution(String substitution) {
        this.substitution = substitution;
    }


    /* (non-Javadoc)
	 * @see xsdvi.svg.AbstractSymbol#draw()
     */
    @Override
    public void draw() {
        // print("<a href=\"#\" onclick=\"window.parent.location.href = window.parent.location.href.split('#')[0]  + '#type_" + name + "'\">");

        processDescription();

        drawGStart();
        print("<rect class='shadow' x='3' y='3' width='" + width + "' height='" + height + "'/>");
        if (optional) {
            print("<rect class='boxelementoptional' x='0' y='0' width='" + width + "' height='" + height + "'");
        } else {
            print("<rect class='boxelement' x='0' y='0' width='" + width + "' height='" + height + "'");
        }
        // all items display, no need to mouseover event
        //drawMouseover();
        print("/>");
        if (namespace != null) {
            print("<text class='visible' x='5' y='13'>" + namespace + "</text>");
        }
        /*if (substitution!=null) {
			print("<text class='hidden' visibility='hidden' x='5' y='13'>subst.: "+substitution+"</text>");
			print("<text class='hidden' visibility='hidden' x='5' y='41'>nillable: "+(nillable ? "1" : "0")+", abstract: "+(abstr ? "1" : "0")+"</text>");
		}
		else {
			print("<text class='hidden' visibility='hidden' x='5' y='13'>nillable: "+(nillable ? "1" : "0")+"</text>");
			print("<text class='hidden' visibility='hidden' x='5' y='41'>abstract: "+(abstr ? "1" : "0")+"</text>");
		}*/
        if (name != null) {
            print("<text class='strong elementlink' x='5' y='27'>" + name + "</text>");
        }

        ArrayList<String> propertiesArray = new ArrayList<>();
        if (cardinality != null) {
            propertiesArray.add(cardinality);
        }
        if (substitution != null) {
            propertiesArray.add("subst.: " + substitution);
        }
        if (abstr) {
            propertiesArray.add("abstract: true");
        }
        String properties = String.join(", ", propertiesArray);
        print("<text x='5' y='59'>" + properties + "</text>");

        drawDescription(59);

        drawConnection();
        drawUse();
        drawGEnd();
        //print("</a>");
    }

    /* (non-Javadoc)
	 * @see xsdvi.svg.AbstractSymbol#getWidth()
     */
    @Override
    public int getWidth() {
        WidthCalculator calc = new WidthCalculator(MIN_WIDTH);
        calc.newWidth(15, name, 3);
        calc.newWidth(15, namespace);
        calc.newWidth(15, cardinality);
        calc.newWidth(15, (substitution == null ? 11 : 22));
        calc.newWidth(15, substitution, 8);
        return calc.getWidth();
    }

    /* (non-Javadoc)
	 * @see xsdvi.svg.AbstractSymbol#getHeight()
     */
    @Override
    public int getHeight() {
        return MAX_HEIGHT;
    }
}
