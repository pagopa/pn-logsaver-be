package it.pagopa.pn.logsaver.config;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.command.base.CommandArgs;
import it.pagopa.pn.logsaver.command.base.CommandArgs.TypeReference;
import it.pagopa.pn.logsaver.command.base.Commands;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.Setter;


@Component
@Setter
public class ClApplicationArguments {

  /*
   * Uso il component Environment invece del component ApplicationArguments in modo da poter gestire
   * command e applicationArgs come variabili di ambiente
   */
  @Autowired
  private Environment env;

  private Commands command;

  @PostConstruct
  void init() {
    this.command = Optional.ofNullable(Commands.getCommand(env.getProperty("nonOptionArgs")))
        .orElse(Commands.DAILY_LOGSAVER);
  }

  public Commands getCommand() {
    return command;
  }

  public List<LocalDate> getDateList() {
    String valueStr = optionValueString(CommandArgs.DATELIST.getArgName(), StringUtils.EMPTY);
    return CommandArgs.DATELIST.convertArgValue(valueStr, new TypeReference<List<LocalDate>>() {});
  }

  public Set<LogFileType> getLogFileTypes() {
    String valueStr = optionValueString(CommandArgs.FILETYPE_LIST.getArgName(), StringUtils.EMPTY);
    return CommandArgs.FILETYPE_LIST.convertArgValue(valueStr,
        new TypeReference<Set<LogFileType>>() {});
  }

  public Map<Retention, Set<ExportType>> getRetentionExportTypesMap() {
    String valueStr =
        optionValueString(CommandArgs.RETENTION_EXPORT_TYPE.getArgName(), StringUtils.EMPTY);
    return CommandArgs.RETENTION_EXPORT_TYPE.convertArgValue(valueStr,
        new TypeReference<Map<Retention, Set<ExportType>>>() {});
  }

  public String getDownloadFolder() {
    String valueStr =
        optionValueString(CommandArgs.DOWNLOAD_FOLDER_DEST.getArgName(), StringUtils.EMPTY);
    return CommandArgs.DOWNLOAD_FOLDER_DEST.convertArgValue(valueStr, String.class);
  }

  public LocalDate getDate() {
    String valueStr = optionValueString(CommandArgs.DATE.getArgName(), StringUtils.EMPTY);
    return CommandArgs.DATE.convertArgValue(valueStr, LocalDate.class);
  }

  public LocalDate getDateFrom() {
    String valueStr = optionValueString(CommandArgs.DATE_FROM.getArgName(), StringUtils.EMPTY);
    return CommandArgs.DATE_FROM.convertArgValue(valueStr, LocalDate.class);
  }

  public LocalDate getDateTo() {
    String valueStr = optionValueString(CommandArgs.DATE_TO.getArgName(), StringUtils.EMPTY);
    return CommandArgs.DATE_TO.convertArgValue(valueStr, LocalDate.class);
  }

  private String optionValueString(String name, String defValue) {
    return StringUtils.isEmpty(env.getProperty(name)) ? defValue : env.getProperty(name);
  }

}
