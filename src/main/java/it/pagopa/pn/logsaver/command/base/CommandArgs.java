package it.pagopa.pn.logsaver.command.base;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import it.pagopa.pn.logsaver.exceptions.InternalException;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@Getter
public enum CommandArgs

{
  DATELIST("date.list", CommandArgsFn.parseDateList), FILETYPE_LIST("log.file.types",
      CommandArgsFn.parseFileTypeList), RETENTION_EXPORT_TYPE("retention.export.type",
          CommandArgsFn.parseRetention), DOWNLOAD_FOLDER_DEST("folder.dest",
              Function.identity()), DATE("date", CommandArgsFn.parseDate), DATE_TO("date.to",
                  CommandArgsFn.parseDate), DATE_FROM("date.from", CommandArgsFn.parseDate);



  private String argName;
  private Function<String, ?> parseFunction;


  private CommandArgs(String name, Function<String, ?> parseFunction) {
    this.argName = name;
    this.parseFunction = parseFunction;
  }

  public <T> T getArgValue(String valueStr, Class<T> type) {
    return type.cast(this.parseFunction.apply(valueStr));
  }

  public <T> T getArgValue(String valueStr, TypeReference<T> type) {
    return type.cast(this.parseFunction.apply(valueStr));
  }

  public abstract static class TypeReference<T> {

    private final Type type;

    protected TypeReference() {
      Type superclass = getClass().getGenericSuperclass();
      type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }

    public Type getType() {
      return type;
    }

    @SuppressWarnings("unchecked")
    public T cast(Object obj) {
      return (T) obj;

    }
  }
  @UtilityClass
  protected static final class CommandArgsFn {
    public static final Function<String, Map<Retention, Set<ExportType>>> parseRetention =
        retentionExportType -> StringUtils.isNoneEmpty(retentionExportType)
            ? Stream.of(StringUtils.split(retentionExportType, ","))
                .map(retExTypes -> StringUtils.split(retExTypes, "$"))
                .collect(groupingBy(arr -> Retention.valueOf(arr[0]),
                    collectingAndThen(toList(),
                        list -> list.stream()
                            .flatMap(el -> Stream.of(StringUtils.split(el[1], "|"))
                                .map(ExportType::valueOf).distinct())
                            .distinct().collect(toSet()))))
            : LogSaverUtils.defaultRetentionExportTypeMap();


    public static final Function<String, List<LocalDate>> parseDateList =
        dateListStr -> StringUtils.isNoneEmpty(dateListStr) ? //
            List.of(dateListStr.split(",")).stream().map(DateUtils::parse)
                .collect(Collectors.toList())
            : new ArrayList<>();

    public static final Function<String, Set<LogFileType>> parseFileTypeList =
        typeListStr -> StringUtils.isNoneEmpty(typeListStr) ? //
            List.of(typeListStr.split(",")).stream().map(LogFileType::valueOf)
                .collect(Collectors.toSet())
            : Set.of(LogFileType.values());

    public static final Function<String, LocalDate> parseDate = dateStr -> {
      if (StringUtils.isNoneEmpty(dateStr)) {
        return DateUtils.parse(dateStr);
      } else {
        throw new InternalException("Argument --date is Empty!");
      }
    };

  }
}
