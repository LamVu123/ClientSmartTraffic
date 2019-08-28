var express = require("express");
var app = express();
var server = require("http").createServer(app);
var io = require("socket.io").listen(server);
var fs = require("fs");
var mysql = require("mysql");
var sql = require("mssql");
var app = express();
var exec = require('child_process').exec;
server.listen(process.env.PORT || 7998);

//connect databases
var conn = mysql.createConnection({
  host: "localhost",
  user: "root",
  password: "root",
  database: "demo",
  port: "3306"
});

conn.connect(function(error){
        if(!!error){
                console.log(error);
        }else{
                console.log("Connected Mysql");
        }
});



//server1 for admin and collect data
io.sockets.on('connection', function (socket) {
	console.log("admin app  connected server to sent data of road");

	socket.on('startSent', function () {
		
		socket.on('roadname2', function (name) {
			socket.road = name;
        })
		
		socket.on('filename2', function (name) {
			socket.name = name;
        });

	//save file
        socket.on('data2',function(data){
                  var mkdirp = require('mkdirp');
                  mkdirp('FolderSave/'+socket.road, function(err) {});
                  fs.writeFile('FolderSave/'+socket.road+'/'+socket.name, data, (err) => {
			// throws an error, you could also catch it here
                        if (err){console.log(err);}
                        // success case, the file was saved
                        console.log('File saved!');
                  });
          });
		
    });
	socket.on('endSent', function () {
	});


 /*       socket.on('listRoad',function(listRoad){
		console.log("getting listRoad");
                conn.query("select * from road",function(error,results,fields){
                        if(!!error){
                                 console.log(error);
                        }
			//console.log(results);
                        console.log("get and sent list road");
                        socket.emit("listRoadResult", {dataListRoad:results});
                 });
        });

	// sent data to admin
        socket.on('idRoad',function(idRoad){
                conn.query("select * from points where idroad  = ? ",[idRoad],function(error,results,fields){
                        if(!!error){
                                 console.log(error);
                        }
                        console.log("get and sent list points");
                        socket.emit("listPointsResult", {points:results});
                 });
        });


        // delete points
        socket.on('idPointDelete',function(idPointDelete){
		console.log("delete points id "+idPointDelete);
                conn.query("delete from points where id = ?",[idPointDelete]);
        });

        //delete road and points of road
        socket.on('roadDelete',function(roadIdDelete){
		//detele file
                conn.query("select * from road where idroad = ?",[roadIdDelete],function(err,result){
                        console.log("result[0].nameroad "+result[0].nameroad);
                        exec('rm -r File/'+result[0].nameroad,function(err, result){
                                console.log("delete file data"+result.toString());
                        });
                });

                //delete points and road in database
                conn.query("delete from points where idroad = ?",[roadIdDelete]);
                conn.query("delete from road where idroad = ?",[roadIdDelete]);
		console.log("delete road "+ roadIdDelete);

        });

	//update road name
	socket.on('updateNameRoad',function(dataUpdate){

		conn.query("select * from road where idroad = ?",[dataUpdate.idRoad],function(err,result){
			//console.log("result[0].nameroad "+result[0].nameroad);
        	        exec('mv File/'+result[0].nameroad+' File/'+dataUpdate.newName,function(err, result){
                	        console.log(result.toString());
	                });
		});
                conn.query("UPDATE road  SET nameroad  = ? where idroad = ?",[dataUpdate.newName,dataUpdate.idRoad]);
		console.log("update name road "+dataUpdate.newName);
	});
	//send data point
	socket.on('viewData',function(id){
		console.log("sending data point id "+id);
	        conn.query("select * from points where id = ?",[id],function(error,resultsFile){
                        if(!!error){
                            	console.log(error);
                       	}
               	        socket.fileName = resultsFile[0].namefiledata;
	               	conn.query("select * from road where idroad = (select idroad from points where id = ? )",[id],function(error,resultsFolder){
                		if(!!error){
                               	        console.log(error);
                      		}
                               	socket.folderName = resultsFolder[0].nameroad;
               		        fs.readFile('File/'+socket.folderName+'/'+socket.fileName, (err, data) => {
                         		if (err) throw err;
                                	socket.emit("dataPoint",{dataResult:data.toString()});
                                	console.log("sent data successfuly");
                        	});
                        });
        	 });
	});


    var idRoadCheck;
	socket.on('road',function (road){
	        conn.query("select * from demo.road where nameroad like ?",[road] ,function(error,results,fields){
			console.log("check road exist");
                 	if(!!error){
                        	console.log(error);
                 	}
                        console.log("In query checking");
			if(results.length > 0){
				console.log("road "+road+" is exist");
				socket.checkRoad = "true";
                		idRoadCheck = results[0].idroad;
			}
			else if(results.length == 0){
				socket.checkRoad = "false";
	                        console.log("road "+road+" is not exist");
			}
                        socket.road= road;
	                if(socket.checkRoad == "true"){
        	                //delete all file
				console.log("delete file in folder File/"+road);
                	        const rimraf = require('rimraf');
                       		rimraf('./File/'+road+'/', function () { console.log('done delete'); });
		                socket.road= road;
	        	        var mkdirp = require('mkdirp');
                		mkdirp('File/'+road, function(err) {
                		});
	               	}
       		 });
	});
	socket.on('filename', function (name) {
		socket.name = name;
         });

	//save file
        socket.on('data',function(data){
                  var mkdirp = require('mkdirp');
                  mkdirp('File/'+socket.road, function(err) {});
                  fs.writeFile('File/'+socket.road+'/'+socket.name, data, (err) => {
			// throws an error, you could also catch it here
                        if (err){console.log(err);}
                        // success case, the file was saved
                        console.log('File saved!');
                  });
          });

	socket.on('end_send_file',function(data){
		sleep(2000);
		console.log("handing data recive");
	 	if(socket.checkRoad == "true"){
			console.log("road is exist " );
			console.log("delete points");
			conn.query("DELETE FROM demo.points where idroad = ?",[idRoadCheck]);

			//handing data
			var roadFolder = socket.road;
			const testFolder = './File/' + roadFolder;
			var readline = require('readline');
			fs.readdirSync(testFolder).forEach(file => {
       				const readLastLines = require('read-last-lines');
				readLastLines.read('./File/' + roadFolder  + '/' + file, 1)
        			.then((lines) => {
                			lines = lines.trim();
                			for(var i = 0; i < 4;i++){
                        			lines = lines.slice(lines.indexOf(" ")+1,lines.length);
                			}

               				var latitude = lines.slice(0,lines.indexOf(" "));
                			lines = lines.slice(lines.indexOf(" ")+1,lines.length);
                			var longitude = lines.slice(0,lines.indexOf(" "));
					lines = lines.slice(lines.indexOf(" ")+1,lines.length);
                			var time = lines.slice(0,lines.indexOf(" "));
					conn.query("INSERT INTO demo.points (idroad,longitude,latitude,timecollect,namefiledata) VALUES (?,?,?,?,?)",[idRoadCheck,longitude,latitude,time,file]);
					console.log("add "+file+" to database" );
       				 });
			});
			console.log("ADD DONE");
		}else if(socket.checkRoad == "false"){
			console.log("road is not exist");
			conn.query("INSERT IGNORE INTO demo.road (nameroad) VALUES ( ? )",[socket.road]);
			var idRoadCheck2;
            		sleep(2000);
			conn.query("select idroad from demo.road where nameroad like ?",[socket.road] ,function(error,results,fields){
	               	        if(!!error){
                    	             console.log(error);
                       		 }
                 	        idRoadCheck2 = results[0].idroad;
	                 });

        	        //handing data
           	        var roadFolder = socket.road;
              	        const testFolder = './File/' + roadFolder;
            		 var readline = require('readline');
               		 fs.readdirSync(testFolder).forEach(file => {

                        	const readLastLines = require('read-last-lines');
                      		 readLastLines.read('./File/' + roadFolder  + '/' + file, 1)
                        	.then((lines) => {
                                	lines = lines.trim();
                                	for(var i = 0; i < 4;i++){
                                        	lines = lines.slice(lines.indexOf(" ")+1,lines.length);
                                	}
                                	var latitude = lines.slice(0,lines.indexOf(" "));
                                	lines = lines.slice(lines.indexOf(" ")+1,lines.length);
                                	var longitude = lines.slice(0,lines.indexOf(" "));
          			        lines = lines.slice(lines.indexOf(" ")+1,lines.length);
                                        var time = lines.slice(0,lines.indexOf(" "));
					console.log("add "+file+" to database");
                                	conn.query("INSERT INTO demo.points (idroad,longitude,latitude,timecollect,namefiledata) VALUES (?,?,?,?,?)",[idRoadCheck2,longitude,latitude,time,file]);
                         	});
                	});
			console.log("ADD DONE");
		}
	});*/
});
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
