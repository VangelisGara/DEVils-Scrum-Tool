# DEVils-Scrum-Tool
Web cooperative platform for software management. This platform helps teams to implement software projects based on agile methodology (SCRUM) via sprints, epics, user stories and issues.

[Youtube Preview](https://www.youtube.com/watch?v=9leSPphzYeI)

## Build and run war in jetty server
    cd ScrumTool
    gradle buildapp
    gradle war
    gradle apprunwar

## Build Project
    cd ScrumTool
    gradle build

## Build Front End
	gradle buildapp		

## Run Project

### Run Back-end
    gradle appRun
  
### Run Front-end
    gradle frontendRun
> To successfully run front-end you must have the latest versions of npm and nodeJS

## Add security exception for SSL certificate
	https://localhost:8443/app/api/users/1/projects (enable backend certificate)
	https://localhost:8080/#/ 			(frontend)
> May require Log Out and then Log In when first run frontend url
