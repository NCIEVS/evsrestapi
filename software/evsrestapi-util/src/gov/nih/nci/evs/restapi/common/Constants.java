package gov.nih.nci.evs.restapi.common;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2017 NGIS. This software was developed in conjunction
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
 *      "This product includes software developed by NGIS and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIS" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIS
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIS, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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
 * Modification history:
 *     Initial implementation kim.ong@ngc.com
 *
 */


public class Constants {
	public static final  String INVERSE_IS_A = "inverse_is_a";
	public static final int TRAVERSE_UP = 1;
	public static final int TRAVERSE_DOWN = 0;
    public static final String NEW_CODE = "NHC0";
    public static final String NCIT_NS = "<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>";

	public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	public static final String INVERSE_IS_OF = "inverseIsA";
	public static final String ROLE = "Role";
	public static final String ASSOCIATION = "Association";
	public static final String HIERARCHICAL = "Hierarchical";

    public static final String FULL_SYN = "FULL_SYN";
    public static final String DEFINITION = "DEFINITION";
    public static final String ALT_DEFINITION = "ALT_DEFINITION";

	public static final String EXACT_MATCH = "exactMatch";
	public static final String STARTS_MATCH = "startsWith";
	public static final String ENDS_MATCH = "endsWith";
	public static final String CONTAINS = "contains";
	public static String DISEASE_IS_STAGE  = "Disease_Is_Stage";


	public static final String EVSRESTAPI_BEAN = "gov.nih.nci.evs.restapi.bean";

    public static String[] COMMON_PROPERTIES = {"code", "label", "Preferred_Name", "Display_Name", "DEFINITION", "ALT_DEFINITION",
                                                "FULL_SYN", "Concept_Status", "Semantic_Type"};

	public static final String[] CTRP_MAIN_CANCER_TYPES = new String[] {
		"C27814", "C2916", "C2946", "C2947", "C2948", "C2955", "C2991", "C2996", "C3011", "C3059",
		"C3088", "C3099", "C3158", "C3161", "C3163", "C3167", "C3171", "C3172", "C3174", "C3178",
		"C3194", "C3208", "C3211", "C3224", "C3230", "C3234", "C3242", "C3247", "C3263", "C3267",
		"C3270", "C3367", "C3403", "C3411", "C3422", "C34448", "C3510", "C3513", "C35850", "C3708",
		"C3716", "C3728", "C3752", "C3753", "C3773", "C3790", "C3809", "C3844", "C3850", "C38661",
		"C3867", "C3917", "C40022", "C4290", "C4436", "C4536", "C4665", "C4699", "C4715", "C4741",
		"C4815", "C4817", "C4855", "C4863", "C4866", "C4872", "C4878", "C4896", "C4906", "C4908",
		"C4910", "C4911", "C4912", "C4914", "C5669", "C6142", "C61574", "C7062", "C7352", "C7539",
		"C7541", "C7558", "C7569", "C7724", "C7927", "C8711", "C8990", "C8993", "C9039", "C9061",
		"C9063", "C9087", "C9106", "C9118", "C9145", "C9272", "C9290", "C9291", "C9306", "C9309",
		"C9312", "C9325", "C9344", "C9349", "C9357", "C9382", "C9385", "C9466", "C9474"};


    /**
     * Constructor
     */
    private Constants() {

    }

} // Class Constants
