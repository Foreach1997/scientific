// 生产静态资源访问
//var dev = "http://localhost:8080/scientific/"
var dev = "http://localhost:8080/"


$(function () {
    var roleId = sessionStorage.getItem("roleId");
    var data;
    $.ajax({
        type: "GET",
        url: dev + "ScientificUser/getUserRoleAuth",
        async: false,
        data: {},
        success: function (res) {
            data = res.data;
        }
    })
    if (roleId == 1){
        $('#admin').show();
        $("#projectType").show();
        $("#performance").show();
        $("#condition").show();
        $("#project").show();
        $("#projectInfo").show()
        $("#addProject").show()
        $("#detail").show()
        $("#performanceStatistics").show()
        $("#personalPerformance").show()
        $("#deptPerformance").show()
    }
    console.log(data[roleId])
   for (const item in data[roleId]) {
       console.log(item)
       if (data[roleId][item] === "projectType") {
           $("#projectInfo").show()
           $("#projectType").show();
       }
       if (data[roleId][item]  === "performance") {
           $("#performance").show();
       }
       if (data[roleId][item]  === "deptPerformance") {
           $("#deptPerformance").show();
       }
       if (data[roleId][item]  === "performanceStatistics") {
           $("#performanceStatistics").show();
       }
       if (data[roleId][item]  === "personalPerformance") {
           $("#personalPerformance").show();
       }
       if (data[roleId][item]  === "condition") {
           $("#projectInfo").show()
           $("#condition").show();
       }
       if (data[roleId][item]  === "project") {
           $("#project").show();
       }
       if (data[roleId][item]  === "addProject") {
           $("#addProject").show()
       }
   };

})