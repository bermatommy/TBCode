USE tbcode;

-- Table for Users
CREATE TABLE Users (
    UserID INT AUTO_INCREMENT PRIMARY KEY,
    Username VARCHAR(255) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL
);

-- Table for Errors
CREATE TABLE Errors (
    ErrorID INT AUTO_INCREMENT PRIMARY KEY,
    UserID INT NOT NULL,
    Header VARCHAR(255),
    Description TEXT,
    Code VARCHAR(50),
    Tags VARCHAR(255),
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE
);

-- Table for Solutions (Now includes UserID)
CREATE TABLE Solutions (
    SolutionID INT AUTO_INCREMENT PRIMARY KEY,
    ErrorID INT, -- Foreign key
    UserID INT NOT NULL, -- Track who submitted the solution
    CodeSolution TEXT,
    Description TEXT,
    SolutionScore INT DEFAULT 0, -- Default score to 0
    ScoreTimes INT DEFAULT 0, -- Default score to 0
    FOREIGN KEY (ErrorID) REFERENCES Errors(ErrorID) ON DELETE CASCADE,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE
);

-- Table for Tags
CREATE TABLE Tags (
    TagID INT AUTO_INCREMENT PRIMARY KEY,
    TagName VARCHAR(255) NOT NULL
);