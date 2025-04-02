const express = require('express');
const mysql = require('mysql2');
const nodemailer = require('nodemailer');
const bcrypt = require('bcryptjs');
const app = express();
const port = 3000;
const path = require('path');
const fs = require('fs');  //filesystem node.js module
const { v4: uuidv4 } = require('uuid'); //uuid library Generates unique identifiers
const multer = require('multer');

// Middleware to parse JSON request bodies
app.use(express.json());

// MySQL connection
const connection = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: 'Kocsi20021217',
    database: 'usedPalace'
});

connection.connect((err) => {
    if (err) throw err;
    console.log('Connected to MySQL database!');
});

// Nodemailer setup
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: 'filmbeadando2024@gmail.com',
        pass: 'dbah zuzh tfly qmcm'
    }
});

// Function to generate a unique 6-digit code FOR PASSWORD
const generateUniqueCode = async () => {
    let code;
    let isUnique = false;

    while (!isUnique) {
        code = Math.floor(100000 + Math.random() * 900000).toString();
        const query = 'SELECT * FROM Users WHERE ForgetToken = ?';
        const [results] = await connection.promise().query(query, [code]);
        if (results.length === 0) {
            isUnique = true;
        }
    }

    return code;
};

// Request password reset
app.post('/forgot-password', async (req, res) => {
    try {
        const { email, phoneNumber } = req.body;
        console.log('Received forgot-password request:', req.body);

        const query = 'SELECT * FROM Users WHERE Email = ? AND PhoneNumber = ?';
        const [results] = await connection.promise().query(query, [email, phoneNumber]);

        if (results.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }

        const user = results[0];
        const code = await generateUniqueCode();

        const updateQuery = 'UPDATE Users SET ForgetToken = ? WHERE Uid = ?';
        await connection.promise().query(updateQuery, [code, user.Uid]);

        const mailOptions = {
            from: 'filmbeadando2024@gmail.com',
            to: email,
            subject: 'Password Reset Code',
            text: `Your password reset code is: ${code}`
        };

        transporter.sendMail(mailOptions, (err, info) => {
            if (err) {
                console.error('Error sending email:', err);
                return res.status(500).json({ error: 'Failed to send email' });
            }
            res.json({ message: 'Reset code sent to your email' });
        });
    } catch (err) {
        console.error('Error in /forgot-password:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// Reset password
app.post('/reset-password', async (req, res) => {
    try {
        const { email, code, newPassword } = req.body;
        console.log('Received reset-password request:', req.body);

        const query = 'SELECT * FROM Users WHERE Email = ? AND ForgetToken = ?';
        const [results] = await connection.promise().query(query, [email, code]);

        if (results.length === 0) {
            return res.status(400).json({ error: 'Invalid or expired code' });
        }

        const user = results[0];
        const hashedPassword = await bcrypt.hash(newPassword, 10);

        const updateQuery = 'UPDATE Users SET PassHashed = ?, ForgetToken = NULL WHERE Uid = ?';
        await connection.promise().query(updateQuery, [hashedPassword, user.Uid]);

        res.json({ message: 'Password reset successfully' });
    } catch (err) {
        console.error('Error in /reset-password:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});


//TODO bruteforce protection with login rate limiter
// Login user
app.post('/login', async (req, res) => {
    const { email, password } = req.body;

    try {
        // Fetch the user from the database
        const query = 'SELECT * FROM Users WHERE Email = ?';
        const [results] = await connection.promise().query(query, [email]);

        if (results.length === 0) {
            return res.status(401).json({ error: 'User not found' });
        }

        const user = results[0];

        // Compare the provided password with the hashed password
        const isPasswordValid = await bcrypt.compare(password, user.PassHashed);

        if (!isPasswordValid) {
            return res.status(401).json({ error: 'Invalid password' });
        }

        // Check if the user is verified
        if (!user.Verified) {
            return res.status(401).json({ error: 'Email not verified. Please verify your email.' });
        }

        // If everything is valid, return success
        const safeUserData = {
   		 id: user.Uid,               
    		name: user.Fullname            
	};

	res.json({ 
    		message: 'Login successful!', 
    		user: safeUserData 
	});
    } catch (err) {
        console.error('Error in /login:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// API to register a new user
app.post('/register', async (req, res) => {
    const { fullname, email, password, phoneNumber } = req.body;

    try {
        // Hash the password
        const saltRounds = 10; // Number of salt rounds (higher = more secure but slower)
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // Insert the user into the database
        const query = 'INSERT INTO Users (Fullname, Email, PassHashed, PhoneNumber) VALUES (?, ?, ?, ?)';
        connection.query(query, [fullname, email, hashedPassword, phoneNumber], (err, results) => {
            if (err) {
                res.status(500).json({ error: 'Failed to register user' });
            } else {
                res.json({ message: 'User registered successfully!' });
            }
        });
    } catch (error) {
        res.status(500).json({ error: 'Failed to hash password' });
    }
});


// Function to generate a unique 6-digit code FOR EMAIL
const generateUniqueCode2 = async () => {
    let code;
    let isUnique = false;

    while (!isUnique) {
        code = Math.floor(100000 + Math.random() * 900000).toString();
        const query = 'SELECT * FROM Users WHERE VerifyToken = ?';
        const [results] = await connection.promise().query(query, [code]);
        if (results.length === 0) {
            isUnique = true;
        }
    }

    return code;
};


// Send Verification email
app.post('/send-verify-email', async (req, res) => {
    try {
        const { email } = req.body;
        console.log('Received send-verify-email request:', req.body);

        // Generate a unique verification code
        const code = await generateUniqueCode2();

        // Save the verification code to the database for the user
        const updateQuery = 'UPDATE Users SET VerifyToken = ? WHERE Email = ?';
        await connection.promise().query(updateQuery, [code, email]);

        // Send the verification email
        const mailOptions = {
            from: 'filmbeadando2024@gmail.com',
            to: email,
            subject: 'Email verification UsedPalace',
            text: `Your email verification code is: ${code}`
        };

        transporter.sendMail(mailOptions, (err, info) => {
            if (err) {
                console.error('Error sending email:', err);
                return res.status(500).json({ error: 'Failed to send email' });
            }
            res.json({ message: 'Verification code sent to your email' });
        });
    } catch (err) {
        console.error('Error in /send-verify-email:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// Verify Email
app.post('/verify-email', async (req, res) => {
    try {
        const { email, code } = req.body;
        console.log('Received verify-email request:', req.body);

        // Check if the email and code match in the database
        const query = 'SELECT * FROM Users WHERE Email = ? AND VerifyToken = ?';
        const [results] = await connection.promise().query(query, [email, code]);

        if (results.length === 0) {
            return res.status(400).json({ error: 'Invalid or expired verification code' });
        }

        const user = results[0];

        // Update the Verified column to true and clear the VerifyToken
        const updateQuery = 'UPDATE Users SET Verified = TRUE, VerifyToken = NULL WHERE Uid = ?';
        await connection.promise().query(updateQuery, [user.Uid]);

        res.json({ message: 'Email verified successfully' });
    } catch (err) {
        console.error('Error in /verify-email:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

//End points for home fragment
// API endpoint to fetch sales data
app.get('/api/sales', (req, res) => {
    const query = 'SELECT * FROM Sales';
    connection.query(query, (err, results) => {
        if (err) {
            console.error('Error fetching sales data:', err);
            res.status(500).json({ error: 'Failed to fetch sales data' });
        } else {
            res.json(results);
        }
    });
});

// Serve static files (e.g., images)
app.use(express.static('sales'));

app.post('/search-sales', async (req, res) => {
    try {
        const { searchParam } = req.body;
        console.log('Received search request:', searchParam);

        if (!searchParam) {
            return res.status(400).json({
                success: false,
                message: "Search parameter is required",
                data: []
            });
        }

        const query = 'SELECT * FROM Sales WHERE Name LIKE ?';
        const searchValue = `%${searchParam}%`;

        const [results] = await connection.promise().query(query, [searchValue]);

        // Ensure we always return the same structure
        res.json({
            success: true,
            message: results.length ? "Products found" : "No products found",
            data: results
        });
        
    } catch (err) {
        console.error('Error in /search-sales:', err);
        res.status(500).json({
            success: false,
            message: "Server error",
            data: []
        });
    }
});

app.post('/search-salesID', async (req, res) => {
    try {
        const { searchParam } = req.body;
        console.log('Received search request:', searchParam);

        if (!searchParam) {
            return res.status(400).json({
                success: false,
                message: "Search parameter is required",
                data: []
            });
        }

        const query = 'SELECT * FROM Sales WHERE Uid = ?';

        const [results] = await connection.promise().query(query, [searchParam]);

        // Ensure we always return the same structure
        res.json({
            success: true,
            message: results.length ? "Products found" : "No products found",
            data: results
        });
        
    } catch (err) {
        console.error('Error in /search-sales:', err);
        res.status(500).json({
            success: false,
            message: "Server error",
            data: []
        });
    }
});


app.post('/create-sale', (req, res) =>  {
    try {
        const { name, description, cost, bigCategory, smallCategory, userId } = req.body;

        // Validate input
        if (!name || !description || !cost || !bigCategory || !userId) {
            return res.status(400).json({
                success: false,
                message: "Missing required fields"
            });
        }

        // Create unique folder name
        const saleFolder = `sale_${uuidv4()}`;
        const saleFolderPath = path.join('sales', saleFolder);

        // Create the folder
        if (!fs.existsSync(saleFolderPath)) {
            fs.mkdirSync(saleFolderPath, { recursive: true });
        }

        // Insert into database using callback style
        const query = `INSERT INTO Sales 
                      (Name, Description, Cost, SaleFolder, BigCategory, SmallCategory, Uid) 
                      VALUES (?, ?, ?, ?, ?, ?, ?)`;
        
        connection.query(query, 
            [name, description, cost, saleFolder, bigCategory, smallCategory || null, userId],
            (err, results) => {
                if (err) {
                    console.error('Error creating sale:', err);
                    return res.status(500).json({ 
                        success: false,
                        message: 'Failed to create sale' 
                    });
                }
                
                res.json({ 
                    success: true,
                    message: 'Sale created successfully!',
                    saleId: results.insertId,
                    saleFolder: saleFolder
                });
            }
        );

    } catch (err) {
        console.error('Error in /create-sale:', err);
        res.status(500).json({ 
            success: false,
            message: 'Internal server error' 
        });
    }
});

//modify sale endpoint
app.put('/modify-sale', (req, res) => {
		try {
        const { name, description, cost, bigCategory, smallCategory, userId } = req.body;
		
		// Validate input
        if (!name || !description || !cost || !bigCategory || !userId) {
            return res.status(400).json({
                success: false,
                message: "Missing required fields"
            });
        }
 
        // First verify the sale belongs to the user
        const [sale] = await connection.promise().query(
            'SELECT Uid FROM Sales WHERE Sid = ?', 
            [saleId]
        );
		
		if (sale.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Sale not found",
                data: null
            });
        }

        if (sale[0].Uid !== userId) {
            return res.status(403).json({
                success: false,
                message: "Unauthorized - you can only modify your own sales",
                data: null
            });
        }
		
		
		
		
		const saleFolder = saleData[0].SaleFolder;

        res.json({
			success: true,
			message: "Sale modified successfully",
			saleId: saleId,        
			saleFolder: saleFolder  
		});

    } catch (err) {
        console.error('Error in /modify-sale:', err);
        res.status(500).json({ 
            success: false,
            message: 'Internal server error',
            error: err.message
        });
    }
});	

//Delete sale end point
app.delete('/delete-sale', async (req, res) => {
    try {
        const { saleId, userId } = req.body;

        // Validate input
        if (!saleId || !userId) {
            return res.status(400).json({
                success: false,
                message: "Both saleId and userId are required",
                data: null
            });
        }

        // First verify the sale belongs to the user
        const [sale] = await connection.promise().query(
            'SELECT Uid FROM Sales WHERE Sid = ?', 
            [saleId]
        );

        if (sale.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Sale not found",
                data: null
            });
        }

        if (sale[0].Uid !== userId) {
            return res.status(403).json({
                success: false,
                message: "Unauthorized - you can only delete your own sales",
                data: null
            });
        }

        // Get sale folder before deletion
        const [saleData] = await connection.promise().query(
            'SELECT SaleFolder FROM Sales WHERE Sid = ?',
            [saleId]
        );

        // Delete from database
        await connection.promise().query(
            'DELETE FROM Sales WHERE Sid = ?',
            [saleId]
        );

        // Delete associated images folder
        if (saleData.length > 0 && saleData[0].SaleFolder) {
            const saleFolderPath = path.join('sales', saleData[0].SaleFolder);
            if (fs.existsSync(saleFolderPath)) {
                fs.rmSync(saleFolderPath, { recursive: true });
            }
        }

        res.json({
            success: true,
            message: "Sale deleted successfully",
            data: {
                deletedSaleId: saleId
            }
        });

    } catch (err) {
        console.error('Error in /delete-sale:', err);
        res.status(500).json({ 
            success: false,
            message: 'Internal server error',
            error: err.message
        });
    }
});


//Image Uploader and stuff for it

//Get images for modify
app.post('/get-images-with-saleId', async (req, res) => {
    try {
        const { searchParam } = req.body;
        
        if (!searchParam) {
            return res.status(400).json({
                success: false,
                message: "Search parameter is required",
                data: null
            });
        }

        const query = 'SELECT * FROM Sales WHERE Sid = ? LIMIT 1';
        const [results] = await connection.promise().query(query, [searchParam]);

        res.json({
            success: true,
            message: results.length ? "Product found" : "No product found",
            data: results[0] || null // Return single object or null
        });
        
    } catch (err) {
        console.error('Error in /search-sales:', err);
        res.status(500).json({
            success: false,
            message: "Server error",
            data: null
        });
    }
});

// Configure storage for uploaded files
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        const saleFolder = req.body.saleFolder;
        if (!saleFolder) {
            return cb(new Error('Sale folder is required'));
        }
        
        const uploadDir = path.join('sales', saleFolder);
        
        // Create directory if it doesn't exist
        if (!fs.existsSync(uploadDir)) {
            fs.mkdirSync(uploadDir, { recursive: true });
        }
        
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        const imageIndex = req.body.imageIndex || '1';
        const ext = path.extname(file.originalname) || '.jpg';
        // Changed from image_${imageIndex} to image${imageIndex}
        const filename = `image${imageIndex}${ext}`;
        cb(null, filename);
    }
});

// File filter to only accept images
const fileFilter = (req, file, cb) => {
    if (file.mimetype.startsWith('image/')) {
        cb(null, true);
    } else {
        cb(new Error('Only image files are allowed!'), false);
    }
};

// Initialize multer with configuration
const upload = multer({ 
    storage: storage,
    fileFilter: fileFilter,
    limits: {
        fileSize: 10 * 1024 * 1024 // 10MB file size limit
    }
});

app.post('/image-uploader', upload.single('image'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({
                success: false,
                message: "No file uploaded or invalid file type"
            });
        }

        const { saleFolder, imageIndex } = req.body;
        
        if (!saleFolder) {
            // Clean up the uploaded file if there's an error
            fs.unlinkSync(req.file.path);
            return res.status(400).json({
                success: false,
                message: "Sale folder is required"
            });
        }

        res.json({
            success: true,
            message: "Image uploaded successfully",
            filename: req.file.filename,
            path: req.file.path,
            saleFolder: saleFolder,
            imageIndex: imageIndex
        });

    } catch (err) {
        console.error('Error in /image-uploader:', err);
        
        // Clean up any uploaded file if error occurred
        if (req.file?.path && fs.existsSync(req.file.path)) {
            fs.unlinkSync(req.file.path);
        }
        
        res.status(500).json({
            success: false,
            message: "Server error during image upload",
            error: err.message
        });
    }
});

// Error handling middleware for multer
app.use((err, req, res, next) => {
    if (err instanceof multer.MulterError) {
        // A Multer error occurred when uploading
        return res.status(400).json({
            success: false,
            message: err.message
        });
    } else if (err) {
        // An unknown error occurred
        return res.status(500).json({
            success: false,
            message: err.message
        });
    }
    next();
});


// Start the server
app.listen(port, () => {
    console.log(`Server running on http://localhost:${port}`);
});