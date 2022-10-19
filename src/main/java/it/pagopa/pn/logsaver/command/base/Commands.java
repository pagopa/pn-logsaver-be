package it.pagopa.pn.logsaver.command.base;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import it.pagopa.pn.logsaver.model.enums.IEnum;
import lombok.Getter;

@Getter
public enum Commands {

  DAILY_LOGSAVER(Commands.DAILY_LOGSAVER_S), //
  DATELIST_LOGSAVER(Commands.DATELIST_LOGSAVER_S), //
  DOWNLOAD_AUDIT(Commands.DOWNLOAD_AUDIT_S);

  public static final String DAILY_LOGSAVER_S = "daily-saver";
  public static final String DATELIST_LOGSAVER_S = "datelist-saver";
  public static final String DOWNLOAD_AUDIT_S = "download";

  private String commandName;
  private List<String> argNames;


  private Commands(String name) {
    this.commandName = name;
  }

  public static Set<String> commandNames() {
    return IEnum.valuesSet(Commands.class).stream().map(Commands::getCommandName)
        .collect(Collectors.toSet());
  }

  public static boolean contains(String commandName) {
    return Commands.commandNames().contains(commandName);
  }

  public static Commands getCommand(String commandName) {
    return IEnum.valuesSet(Commands.class).stream()
        .filter(cmd -> cmd.getCommandName().equalsIgnoreCase(commandName)).findFirst()
        .orElseThrow();
  }


}
