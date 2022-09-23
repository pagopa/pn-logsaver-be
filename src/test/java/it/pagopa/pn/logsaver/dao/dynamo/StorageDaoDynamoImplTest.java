package it.pagopa.pn.logsaver.dao.dynamo;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"aws.region-code=us-east-1",
    "aws.profile-name=${PN_AWS_PROFILE_NAME:default}", "aws.endpoint-url=http://localhost:4566",
    "pn.delivery.notification-dao.table-name=Notifications",
    "pn.delivery.notification-cost-dao.table-name=NotificationsCost",
    "pn.delivery.notification-metadata-dao.table-name=NotificationsMetadata"})
@SpringBootTest
public class StorageDaoDynamoImplTest {



}
