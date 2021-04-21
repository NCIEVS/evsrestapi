package gov.nih.nci.evs.restapi.bean;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008,2009 NGIT. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by NGIT and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIT" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIT
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIT, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 *          Modification history Initial implementation kim.ong@ngc.com
 *
 */

public class DisplayItem {
    private String _propertyName;
    private String _itemLabel;
    private String _url;
    private String _hyperlinkText;
    private boolean _isExternalCode;

    public DisplayItem() {
    }

    public DisplayItem(String propertyName, String itemLabel, String url,
        String hyperlinkText) {
        _propertyName = propertyName;
        _itemLabel = itemLabel;
        _url = url;
        _hyperlinkText = hyperlinkText;
        _isExternalCode = false;
    }

    public DisplayItem(String propertyName, String itemLabel, String url,
        String hyperlinkText, boolean isExternalCode) {
        _propertyName = propertyName;
        _itemLabel = itemLabel;
        _url = url;
        _hyperlinkText = hyperlinkText;
        _isExternalCode = isExternalCode;
    }

    public String getPropertyName() {
        return _propertyName;
    }

    public void setPropertyName(String propertyName) {
        _propertyName = propertyName;
    }

    public String getItemLabel() {
        return _itemLabel;
    }

    public void setItemLabel(String itemLabel) {
        _itemLabel = itemLabel;
    }

    public String getUrl() {
        return _url;
    }

    public void setUrl(String url) {
        _url = url;
    }

    public String getHyperlinkText() {
        return _hyperlinkText;
    }

    public void setHyperlinkText(String hyperlinkText) {
        _hyperlinkText = hyperlinkText;
    }

    public boolean getIsExternalCode() {
        return _isExternalCode;
    }

    public void setIsExternalCode(String isExternalCode) {
        _hyperlinkText = isExternalCode;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("DisplayItem ");
        sb.append("\n");
        sb.append("\tpropertyName: " + getPropertyName());
        sb.append("\n");
        sb.append("\titemLabel: " + getItemLabel());
        sb.append("\n");
        sb.append("\turl: " + getUrl());
        sb.append("\n");
        sb.append("\thyperlinkText: " + getHyperlinkText());
        sb.append("\n");
        sb.append("\tisExternalCode: " + getIsExternalCode());
        sb.append("\n");
        return sb.toString();
    }
}
