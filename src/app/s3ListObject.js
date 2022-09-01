const AWSXRay = require('aws-xray-sdk-core');
const AWS = AWSXRay.captureAWS(require('aws-sdk'));
AWS.config.update({region: 'eu-south-1'});


module.exports = {
    listObjectFromS3(bucketName,prefix) {
        // Create S3 service object
        s3 = new AWS.S3({apiVersion: '2006-03-01'});
        
        // Create the parameters for calling listObjects
        var bucketParams = {
            Bucket : bucketName,
            Prefix: prefix
        };
        console.log("bucketParams: ", bucketParams);
        
        // Call S3 to obtain a list of the objects in the bucket
        return s3.listObjects(bucketParams).promise();
    }
}
