import { GetObjectCommand } from "@aws-sdk/client-s3";
import { PutItemCommand } from "@aws-sdk/client-dynamodb";
import { ddbClient } from "./ddbClient.js";
import { s3Client } from "./s3Client.js";

export const bucketParams = {
    Bucket: "BUCKET_NAME",
    Key: "KEY",
};

export const params = {
    TableName: "TABLE_NAME",
    Item: {
        CUSTOMER_ID: { N: "001" },
        CUSTOMER_NAME: { S: "Richard Roe" },
    },
};

module.exports = {
    async handleEvent(event){
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
