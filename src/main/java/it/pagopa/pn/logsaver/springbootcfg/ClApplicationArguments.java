package it.pagopa.pn.logsaver.springbootcfg;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import it.pagopa.pn.logsaver.utils.LsUtils;
import lombok.Data;


@Configuration
@Data
public class ClApplicationArguments {

  private List<LocalDate> dateList;

  private Set<ItemType> itemTypes;

  private Map<Retention, Set<ExportType>> retentionExportTypesMap;

  @Autowired
  void initDateList(
      @Value("${dateList:}#{T(java.util.Collections).emptyList()}") List<String> dateListStr) {
    this.dateList = dateListStr.stream().map(DateUtils::parse).collect(Collectors.toList());
  }

  @Autowired
  void initTypeList(
      @Value("${types:}#{T(it.pagopa.pn.logsaver.model.ItemType).valuesAsString()}") List<String> typeListStr) {
    this.itemTypes = typeListStr.stream().map(ItemType::valueOf).collect(Collectors.toSet());
  }

  @Autowired
  void initRetentionExportType(@Value("${exportType:}") String retentionExportType) {

    this.retentionExportTypesMap = StringUtils.isNoneEmpty(retentionExportType)
        ? Stream.of(StringUtils.split(retentionExportType, ","))
            .map(retExTypes -> StringUtils.split(retExTypes, "$"))
            .collect(groupingBy(arr -> Retention.valueOf(arr[0]),
                collectingAndThen(toList(),
                    list -> list.stream()
                        .flatMap(el -> Stream.of(StringUtils.split(el[1], "|"))
                            .map(ExportType::valueOf).distinct())
                        .distinct().collect(toSet()))))
        : LsUtils.defaultRetentionExportTypeMap();
  }

  @PostConstruct
  void validate() {

  }
}
