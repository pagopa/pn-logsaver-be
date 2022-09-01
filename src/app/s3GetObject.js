const AWSXRay = require('aws-xray-sdk-core');
const AWS = AWSXRay.captureAWS(require('aws-sdk'));
AWS.config.update({region: 'eu-south-1'});

module.exports = {
    getObjectFromS3(bucketName,key) {
        // Create S3 service object
        s3 = new AWS.S3({apiVersion: '2006-03-01'});
        
        // Create the parameters for calling getObject
        var params = {
            Bucket : bucketName,
            Key: key
        };
        console.log("params: ", params);
        
        // Call S3 to obtain the object in the bucket
        return s3.getObject(params).promise();
    }
}