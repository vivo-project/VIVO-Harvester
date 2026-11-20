package org.vivoweb.harvester.util;

public class XMLTagIndexing {

    private String xmlTagName = null;
    private int elementNo = 0;
    private boolean arrayIndexOpen = false;

    public String getXmlTagName() {
        return xmlTagName;
    }

    public void setXmlTagName(String tagName) {
        this.xmlTagName = tagName;
    }

    public int getElementNo() {
        return elementNo;
    }

    public void setElementNo(int elementNo) {
        this.elementNo = elementNo;
    }

    public void increaseElementNo() {
        this.elementNo++;
    }

    public boolean isArrayIndexOpen() {
        return arrayIndexOpen;
    }

    public void setArrayIndexOpen() {
        this.arrayIndexOpen = true;
    }

    public void setArrayIndexClosed() {
        this.arrayIndexOpen = false;
    }
}
