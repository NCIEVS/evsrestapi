package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.util.EVSUtils;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Implementation class for the terminology suggestion form service. */
@Service
public class TermSuggestionFormServiceImpl implements TermSuggestionFormService {
  /** The Constant logger. */
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(TermSuggestionFormServiceImpl.class);

  /** The mail sender. */
  // JavaMailSender
  private final JavaMailSender mailSender;

  /** The application properties. */
  // The application properties
  private final ApplicationProperties applicationProperties;

  /** The form file path. */
  // path for the form file
  URL formFilePath;

  /** The object mapper to read the config url with readTree. */
  private final ObjectMapper mapper = new ObjectMapper();

  /** Pattern for optional instruction sheets with date suffix */
  private static final Pattern INSTRUCTION_PATTERN =
      Pattern.compile(".*\\d{4}_\\d{2}_\\d{2} Instructions$");

  /**
   * Constructor: Instantiates dependencies.
   *
   * @param mailSender java mail sender
   * @param applicationProperties the application properties
   * @param mapper the mapper
   */
  public TermSuggestionFormServiceImpl(
      final JavaMailSender mailSender,
      final ApplicationProperties applicationProperties,
      final ObjectMapper mapper) {
    this.mailSender = mailSender;
    this.applicationProperties = applicationProperties;
  }

  /**
   * Gets the term suggestion form based on the formType, reads the json, and returns the template
   * as JsonObject.
   *
   * @param formType form template to load
   * @return JsonNode
   * @throws Exception the exception
   */
  @Override
  public JsonNode getFormTemplate(final String formType) throws Exception {
    // Set the form file path based on the formType passed. If we receive an invalid path, throw
    // exception
    if (formType == null || formType.isEmpty() || formType.isBlank()) {
      throw new IllegalArgumentException("Invalid form template provided");
    }
    // Create objectMapper. Read file and return JsonNode
    try {
      String json =
          EVSUtils.getValueFromFile(
              applicationProperties.getConfigBaseUri() + "/" + formType + ".json");
      logger.info("Length: " + json.length());
      logger.info("Starts with: [" + json.substring(0, Math.min(10, json.length())) + "]");
      final JsonNode termForm = mapper.readTree(json);
      // Get the recaptcha_site_key from application properties
      final String recaptchaSiteKey = applicationProperties.getRecaptchaSiteKey();

      // Check our termForm is an object node to safely add properties
      if (termForm.isObject()) {
        ((ObjectNode) termForm).put("recaptchaSiteKey", recaptchaSiteKey);
      } else {
        logger.error("Cannot add recaptcha site key. Form template is not a JSON object.");
        throw new IllegalArgumentException("Invalid form template.");
      }

      return termForm;
    } catch (FileNotFoundException e) {
      logger.error("Form template file not found for type: {}", formType, e);
      throw e;
    }
  }

  /**
   * Sends an email.
   *
   * @param emailDetails details of the email created from the form data
   * @throws MessagingException the messaging exception
   */
  @Override
  public void sendEmail(final EmailDetails emailDetails) throws MessagingException, Exception {
    // Check if starttls.enable is false
    if (mailSender instanceof JavaMailSenderImpl) {
      final Properties mailProperties = ((JavaMailSenderImpl) mailSender).getJavaMailProperties();
      final String starttls = mailProperties.getProperty("mail.smtp.starttls.enable");
      // check we want to start the tls
      if ("false".equals(starttls)) {
        throw new MessagingException(
            "Unable to start TLS to send on a secure connection, aborting send email");
      }
    }
    try {
      // Create the MimeMessage
      final MimeMessage message = mailSender.createMimeMessage();

      // If application properties has an override for the mail recipient, use it
      // this is to allow dev testing to send to a different account than specified
      // in the form.  This also ensures in deployment environment that random
      // emails cannot be specified in the form as a way to spam
      if (applicationProperties.getMailRecipient() != null
          && !applicationProperties.getMailRecipient().isEmpty()) {
        emailDetails.setToEmail(applicationProperties.getMailRecipient());
      }

      logger.info(
          "   Sending email for {} form to {}",
          emailDetails.getSource(),
          emailDetails.getToEmail());
      // Set the email details
      message.setRecipients(RecipientType.TO, emailDetails.getToEmail());
      message.setFrom(new InternetAddress(emailDetails.getFromEmail()));
      message.setSubject(emailDetails.getSubject());
      if (emailDetails.getMsgBody() != null
          && emailDetails.getMsgBody().toLowerCase().contains("<html")) {
        message.setContent(emailDetails.getMsgBody(), "text/html; charset=utf-8");
      } else {
        message.setText(String.valueOf(emailDetails.getMsgBody()));
      }
      mailSender.send(message);
    } catch (final MessagingException e) {
      throw new MessagingException("Failed to send email", e);
    }
  }

  /**
   * Sends an email with an optional file attachment.
   *
   * @param emailDetails details of the email created from the form data
   * @param file optional multipart file to attach
   * @throws MessagingException the messaging exception
   */
  @Override
  public void sendEmailWithAttachment(final EmailDetails emailDetails, final MultipartFile file)
      throws MessagingException, Exception {
    // Check if starttls.enable is false
    if (mailSender instanceof JavaMailSenderImpl) {
      final Properties mailProperties = ((JavaMailSenderImpl) mailSender).getJavaMailProperties();
      final String starttls = mailProperties.getProperty("mail.smtp.starttls.enable");
      // check we want to start the tls
      if ("false".equals(starttls)) {
        throw new MessagingException(
            "Unable to start TLS to send on a secure connection, aborting send email");
      }
    }
    try {
      final MimeMessage message = mailSender.createMimeMessage();

      // If application properties has an override for the mail recipient, use it
      // this is to allow dev testing to send to a different account than specified
      // in the form.  This also ensures in deployment environment that random
      // emails cannot be specified in the form as a way to spam
      if (applicationProperties.getMailRecipient() != null
          && !applicationProperties.getMailRecipient().isEmpty()) {
        emailDetails.setToEmail(applicationProperties.getMailRecipient());
      }

      logger.info(
          "   Sending email for {} form to {}",
          emailDetails.getSource(),
          emailDetails.getToEmail());

      // true indicates multipart message
      final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(emailDetails.getToEmail());
      helper.setFrom(emailDetails.getFromEmail());
      helper.setSubject(emailDetails.getSubject());
      if (emailDetails.getMsgBody() != null
          && emailDetails.getMsgBody().toLowerCase().contains("<html")) {
        helper.setText(emailDetails.getMsgBody(), true);
      } else {
        helper.setText(emailDetails.getMsgBody(), false);
      }

      if (file != null && !file.isEmpty()) {
        try {
          helper.addAttachment(file.getOriginalFilename(), file);
        } catch (final MessagingException me) {
          throw new MessagingException("Failed to attach file to email", me);
        }
      }

      mailSender.send(message);
    } catch (final MessagingException e) {
      throw new MessagingException("Failed to send email", e);
    }
  }

  /**
   * Validate file attachment.
   *
   * @param file the file
   * @return true, if successful
   */
  @Override
  public boolean validateFileAttachment(final MultipartFile file) {
    if (file == null || file.isEmpty()) {
      // No file attached/empty file
      return false;
    }

    // Check file extension - expect .xls/xlsx (case-insensitive)
    String filename = file.getOriginalFilename();
    if (filename == null || filename.isBlank()) {
      filename = file.getName();
    }
    if (filename == null
        || !(filename.toLowerCase().endsWith(".xls") || filename.toLowerCase().endsWith(".xlsx"))) {
      return false;
    }

    // Try to open the workbook once to ensure it's a valid Excel file and collect sheet names
    final java.util.Set<String> sheets = new java.util.HashSet<>();
    try (final java.io.InputStream is = file.getInputStream();
        final Workbook wb = WorkbookFactory.create(is)) {
      // too many sheets, no reason to have extras
      if (wb.getNumberOfSheets() > 6) {
        logger.warn("Too many sheets (>6) in uploaded workbook: {}", filename);
        return false;
      }
      for (int i = 0; i < wb.getNumberOfSheets(); i++) {
        sheets.add(wb.getSheetName(i));
      }

      // Build a set of expected names
      final java.util.Set<String> expectedSet =
          new java.util.HashSet<>(
              Arrays.asList(
                  "Exist Codelist - New Test PARM",
                  "Exist Codelist - New Term",
                  "Changes to Existing Term",
                  "New Codelist - Test or PARM",
                  "New Codelist - New Terms"));

      // We do NOT require that all expected sheets exist.
      // Instead, every sheet present in the workbook must either be one of the expected
      // sheet names or match the instructions date pattern.
      for (final String actual : sheets) {
        if (expectedSet.contains(actual)) {
          continue;
        }
        final Matcher m = INSTRUCTION_PATTERN.matcher(actual);
        if (m.find()) {
          continue;
        }
        logger.warn("Unexpected sheet '{}' found in uploaded workbook: {}", actual, filename);
        return false;
      }

      // We do require New Codelist - Test or PARM exists.
      if (!sheets.contains("New Codelist - Test or PARM")) {
        logger.warn(
            "Required sheet 'New Codelist - Test or PARM' not found in uploaded workbook: {}",
            filename);
        return false;
      }

      // Validate that the New Codelist - Test or PARM sheet has all the necessary information
      final String sheetName = "New Codelist - Test or PARM";
      final org.apache.poi.ss.usermodel.Sheet metaSheet = wb.getSheet(sheetName);
      if (metaSheet == null) {
        logger.warn(
            "Required sheet '{}' missing content in uploaded workbook: {}", sheetName, filename);
        return false;
      }
      final org.apache.poi.ss.usermodel.DataFormatter formatter =
          new org.apache.poi.ss.usermodel.DataFormatter();

      // Handle merged cells for C3:F3, C4:F4, C5:F5 by checking the merged region's first cell
      for (int r = 2; r <= 4; r++) {
        final String cellValue = getMergedCellValue(metaSheet, r, 2, formatter);
        if (cellValue == null || cellValue.isEmpty()) {
          logger.warn(
              "Required metadata missing in sheet '{}' at C{} in uploaded workbook: {}",
              sheetName,
              r + 1,
              filename);
          return false;
        }
      }

      // Check that A8..E8 (columns 0..4, row index 7) are filled out
      for (int c = 0; c <= 4; c++) {
        final String v = getMergedCellValue(metaSheet, 7, c, formatter);
        if (v == null || v.isEmpty()) {
          logger.warn(
              "Required row 8 column {} missing in sheet '{}' in uploaded workbook: {}",
              c + 1,
              sheetName,
              filename);
          return false;
        }
      }
    } catch (final Exception e) {
      logger.warn("Invalid excel file uploaded or failed to validate workbook: {}", filename, e);
      return false;
    }

    return true;
  }

  /**
   * Read a cell value and correctly handle merged regions. Returns trimmed string or empty string.
   */
  private String getMergedCellValue(
      final org.apache.poi.ss.usermodel.Sheet sheet,
      final int rowIndex,
      final int colIndex,
      final org.apache.poi.ss.usermodel.DataFormatter formatter) {
    for (final org.apache.poi.ss.util.CellRangeAddress region : sheet.getMergedRegions()) {
      if (region.isInRange(rowIndex, colIndex)) {
        final org.apache.poi.ss.usermodel.Row firstRow = sheet.getRow(region.getFirstRow());
        if (firstRow == null) {
          return "";
        }
        final org.apache.poi.ss.usermodel.Cell firstCell =
            firstRow.getCell(region.getFirstColumn());
        if (firstCell == null) {
          return "";
        }
        return formatter.formatCellValue(firstCell).trim();
      }
    }
    final org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
    if (row == null) {
      return "";
    }
    final org.apache.poi.ss.usermodel.Cell cell = row.getCell(colIndex);
    if (cell == null) {
      return "";
    }
    return formatter.formatCellValue(cell).trim();
  }
}
