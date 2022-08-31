import { GetObjectCommand } from "@aws-sdk/client-s3";
import { PutItemCommand } from "@aws-sdk/client-dynamodb";
import { ddbClient } from "./ddbClient.js";
import { s3Client } from "./s3Client.js";

const BUCKET_NAME = process.env.BUCKET_NAME
const TABLE_NAME = process.env.TABLE_NAME

export const bucketParams = {
    Bucket: BUCKET_NAME,
    Key: "KEY",
};

export const params = {
    TableName: TABLE_NAME,
    Item: {
        CUSTOMER_ID: { N: "001" },
        CUSTOMER_NAME: { S: "Richard Roe" },
    },
};

const event_example = {
    "id": "cdc73f9d-aea9-11e3-9d5a-835b769c0d9c",
    "detail-type": "Scheduled Event",
    "source": "aws.events",
    "account": "123456789012",
    "time": "1970-01-01T00:00:00Z",
    "region": "us-east-1",
    "resources": [
        "arn:aws:events:us-east-1:123456789012:rule/ExampleRule"
    ],
    "detail": {}
}

module.exports = {
    async handleEvent(event){
        const parsingDate = Date.parse(event.time);

        
        let year = parsingDate.getFullYear();
        let month = parsingDate.getMonth();
        let day = parsingDate.getDay();


        const stringFile = getFileFromS3(bucketParams);
        const data = putItem(params);
    }
}

function getFileFromS3(bucketParams) {
    try {
        const streamToString = (stream) =>
        new Promise((resolve, reject) => {
            const chunks = [];
            stream.on("data", (chunk) => chunks.push(chunk));
            stream.on("error", reject);
            stream.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
        });
        
        const data = await s3Client.send(new GetObjectCommand(bucketParams));
        const bodyContents = await streamToString(data.Body);
        console.log(bodyContents);
        return bodyContents;
    } catch (err) {
        console.log("Error", err);
    }
}

function putItem(params) {
    try {
        const data = await ddbClient.send(new PutItemCommand(params));
        console.log(data);
        return data;
    } catch (err) {
        console.error(err);
    }
}
