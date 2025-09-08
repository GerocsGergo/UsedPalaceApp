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
const jwt = require('jsonwebtoken');
const validator = require('validator');
const WebSocket = require('ws');
const admin = require('firebase-admin');

const userApiErrorMessage = "Ismeretlen hiba történt!"; // A felhasználónak hiba esetén ezeket küldi vissza
const userServerErrorMessage = "Szerver hiba, próbálja újra későuserServerErrorMessage";

require('dotenv').config();
const JWT_SECRET = process.env.JWT_SECRET;


// Middleware to parse JSON request bodies
app.use(express.json());

// Serve static files (e.g., images)
//app.use(express.static('sales'));
app.use('/sales', express.static('sales'));


// MySQL connection
const connection = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

connection.connect((err) => {
    if (err) throw err;
    console.log('Connected to MySQL database!');
});



//Token ellenőrző
const authenticateToken = async (req, res, next) => {
  const token = req.headers.authorization?.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'No token provided' });

  try {
    const decoded = jwt.verify(token, JWT_SECRET);

    // ellenőrizheted a sessiont is adatbázisból
    const [sessions] = await connection.promise().query(
      'SELECT * FROM Sessions WHERE Token = ? AND ExpiresAt > NOW()',
      [token]
    );

    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Invalid or expired session' });
    }

    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
};

//Token ellenőrző használata
//app.get('/protected-data', authenticateToken, (req, res) => {
  //res.json({ data: 'secret', userId: req.user.id });
//});


// Nodemailer setup
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS
    }
});

// Function to generate a unique 6-digit code FOR PASSWORD
const generateUniqueCode = async () => {
    let code;
    let isUnique = false;

    while (!isUnique) {
        // 6 jegyű véletlen szám
        code = Math.floor(100000 + Math.random() * 900000).toString();

        // Új tábla ellenőrzése
        const query = 'SELECT * FROM ForgotPasswordRequests WHERE VerifyToken = ?';
        const [results] = await connection.promise().query(query, [code]);

        if (results.length === 0) {
            isUnique = true;
        }
    }

    return code;
};

// Request forgot password
app.post('/forgot-password', async (req, res) => {
    try {
        const { email, phoneNumber } = req.body;

        if (!email || !phoneNumber) {
            return res.status(400).json({ 
                error: 'Some fields are empty!', 
                message: 'Kérlek töltsd ki az összes mezőt!' 
            });
        }

        if (!validator.isEmail(email)) {
            return res.status(400).json({ 
                error: 'Invalid email address.', 
                message: 'Érvénytelen email cím.' 
            });
        }

        if (phoneNumber.trim().length !== 11) {
            return res.status(400).json({ 
                error: 'Phone number format invalid.', 
                message: 'A telefonszám formátuma nem megfelelő.' 
            });
        }

        const phoneRegex = /^06\d{9}$/;
        if (!phoneRegex.test(phoneNumber)) {
            return res.status(400).json({ 
                error: 'Phone number format invalid.', 
                message: 'A telefonszám formátuma nem megfelelő.' 
            });
        }

        // Find user
        const [users] = await connection.promise().query(
            'SELECT * FROM Users WHERE Email = ? AND PhoneNumber = ?',
            [email, phoneNumber]
        );

        if (users.length === 0) {
            return res.status(404).json({ 
                error: 'User not found', 
                message: userApiErrorMessage

            });
        }

        const user = users[0];
        const code = await generateUniqueCode(); // Pl. random 6 számjegy

        // Insert into ForgotPasswordRequests
        await connection.promise().query(
            'INSERT INTO ForgotPasswordRequests (Uid, VerifyToken) VALUES (?, ?)',
            [user.Uid, code]
        );

        // Send email
        const mailOptions = {
            from: 'filmbeadando2024@gmail.com',
            to: email,
            subject: 'Password Reset Code',
            text: `Your password reset code is: ${code}`
        };

        transporter.sendMail(mailOptions, (err) => {
            if (err) {
                console.error('Error sending email:', err);
                return res.status(500).json({ 
                    error: 'Failed to send email', 
                    message: userApiErrorMessage 
                });
            }
            res.json({ 
                message: 'Reset code sent to your email' 
            });
        });

    } catch (err) {
        console.error('Error in /forgot-password:', err);
        res.status(500).json({ 
             error: err.message,
            message: userServerErrorMessage
        });
    }
});



// Confirm forgot password
app.post('/confirm-forgot-password', async (req, res) => {
    try {
        const { email, code, newPassword } = req.body;

        if (!email || !code || !newPassword) {
            return res.status(400).json({ 
                error: 'Some fields are empty!', 
                message: 'Kérlek töltsd ki az összes mezőt!' 
            });
        }

        if (!validator.isEmail(email)) {
            return res.status(400).json({ 
                error: 'Invalid email address.', 
                message: 'Érvénytelen email cím.' 
            });
        }

        if (newPassword.trim().length < 8) {
            return res.status(400).json({ 
                error: 'Password must be at least 8 characters.', 
                message: 'A jelszónak legalább 8 karakter hosszúnak kell lennie.' 
            });
        }

        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
        if (!passwordRegex.test(newPassword)) {
            return res.status(400).json({
                error: 'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character.',
                message: 'A jelszónak tartalmaznia kell legalább egy nagybetűt, egy kisbetűt, egy számot és egy speciális karaktert.'
            });
        }

        // Find request and user
        const [results] = await connection.promise().query(`
            SELECT u.Uid FROM Users u
            JOIN ForgotPasswordRequests fpr ON u.Uid = fpr.Uid
            WHERE u.Email = ? AND fpr.VerifyToken = ?`,
            [email, code]
        );

        if (results.length === 0) {
            return res.status(400).json({ 
                error: 'Invalid or expired code', 
                message: 'Érvénytelen vagy lejárt kód.' 
            });
        }

        const userId = results[0].Uid;
        const hashedPassword = await bcrypt.hash(newPassword, 10);

        // Update password
        await connection.promise().query(
            'UPDATE Users SET PassHashed = ? WHERE Uid = ?',
            [hashedPassword, userId]
        );

        // Delete request row
        await connection.promise().query(
            'DELETE FROM ForgotPasswordRequests WHERE Uid = ?',
            [userId]
        );

        res.json({ message: 'Password reset successfully' });

    } catch (err) {
        console.error('Error in /confirm-forgot-password:', err);
        res.status(500).json({ 
             error: err.message,
            message: userServerErrorMessage 
        });
    }
});



//TODO bruteforce protection with login rate limiter
// Login user
app.post('/login', async (req, res) => {
    
    try {
		const { email, password, deviceInfo } = req.body;
		console.log('Received login request:', email);
		
		if (!email || !password || !deviceInfo) {
        return res.status(400).json({ 
			error: 'Some data is missing',
			message: 'Kérjük töltsön ki minden mezőt!'});
    }
		
		if (!validator.isEmail(email)) {
        return res.status(400).json({ 
			error: 'Invalid email address.',
			message: 'Érvénytelen E-mail cím!'});
		}
		
        const query = 'SELECT * FROM Users WHERE Email = ?';
        const [results] = await connection.promise().query(query, [email]);

        if (results.length === 0) {
            return res.status(401).json({ 
				error: 'User not found',
				message: 'Nincs ilyen felhasználó'});
        }

        const user = results[0];

        const isPasswordValid = await bcrypt.compare(password, user.PassHashed);
        if (!isPasswordValid) {
            return res.status(401).json({ 
			error: 'Invalid password',
			message: 'Helytelen jelszó'});
        }

        if (!user.Verified) {
            return res.status(401).json({ 
				error: 'Email not verified',
				message: 'Nincs hitelesítve az email'});
        }

        const token = jwt.sign({ id: user.Uid }, JWT_SECRET, { expiresIn: '7d' }); //10s for testing 7d for production
		
		const createdAt = new Date();
        const expiresAt = new Date(createdAt.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 * 24 * 60 * 60 * 1000 = 7d for production // 

		await connection.promise().query(
            'INSERT INTO Sessions (UserId, Token, DeviceInfo, CreatedAt, ExpiresAt) VALUES (?, ?, ?, ?, ?)',
            [user.Uid, token, deviceInfo || 'Unknown', createdAt, expiresAt]
        );
		
        const safeUserData = {
            id: user.Uid,
            name: user.Fullname
        };

        res.json({
            message: 'Sikeres belépés!',
            token,
            user: safeUserData
        });

    } catch (err) {
        console.error('Error in /login:', err);
        res.status(500).json({ 
			error: err.message,
			message: userServerErrorMessage
		});
    }
});

// Check if login token is expired
app.get('/verify-token', async (req, res) => {
    const token = req.headers.authorization?.split(' ')[1];

    if (!token) {
        return res.status(401).json({ 
            error: 'No token provided', 
            message: 'Kérlek jelentkezz be újra.' 
        });
    }

    try {
        const decoded = jwt.verify(token, JWT_SECRET);

        // Session ellenőrzés
        const [sessions] = await connection.promise().query(
            'SELECT * FROM Sessions WHERE Token = ? AND ExpiresAt > NOW()',
            [token]
        );

        if (sessions.length === 0) {
            return res.status(401).json({ 
                error: 'Invalid or expired session', 
                message: 'Érvénytelen vagy lejárt munkamenet. Kérlek jelentkezz be újra.' 
            });
        }

        res.json({ 
            valid: true, 
            user: decoded, 
            message: 'Token érvényes.' 
        });

    } catch (err) {
        if (err.name === 'TokenExpiredError') {
            res.status(401).json({ 
                error: 'Token expired', 
                message: 'A token lejárt. Kérlek jelentkezz be újra.' 
            });
        } else {
            res.status(401).json({ 
                error: 'Invalid token', 
                message: 'Érvénytelen token. Kérlek jelentkezz be újra.' 
            });
        }
    }
});

// Logout endpoint
app.delete('/logout', authenticateToken, async (req, res) => {
    const token = req.headers.authorization?.split(' ')[1];

    if (!token) {
        return res.status(400).json({ 
            error: 'No token provided', 
            message: userApiErrorMessage
        });
    }

    try {
        await connection.promise().query('DELETE FROM Sessions WHERE Token = ?', [token]);
        res.json({ 
            message: 'Sikeres kijelentkezés.' 
        });

    } catch (err) {
        console.error('Error in /logout:', err);
        res.status(500).json({ 
             error: err.message,
            message: userServerErrorMessage 
        });
    }
});


// API to register a new user
app.post('/register', async (req, res) => {
    const { fullname, email, password, phoneNumber } = req.body;
    console.log('Received registration request:', email);

    if (!fullname || !email || !password || !phoneNumber) {
        return res.status(400).json({ 
            error: 'Some fields are empty!', 
            message: 'Minden mező kitöltése kötelező.' 
        });
    }

    if (fullname.trim().length < 2 || fullname.trim().length > 50) {
        return res.status(400).json({ 
            error: 'Fullname length invalid', 
            message: 'A teljes névnek 2 és 50 karakter között kell lennie.' 
        });
    }

    if (password.trim().length < 8) {
        return res.status(400).json({ 
            error: 'Password too short', 
            message: 'A jelszónak legalább 8 karakter hosszúnak kell lennie.' 
        });
    }

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!passwordRegex.test(password)) {
        return res.status(400).json({ 
            error: 'Password complexity invalid', 
            message: 'A jelszónak tartalmaznia kell legalább egy nagybetűt, egy kisbetűt, egy számot és egy speciális karaktert.' 
        });
    }

    if (!validator.isEmail(email)) {
        return res.status(400).json({ 
            error: 'Invalid email address', 
            message: 'Érvénytelen email cím.' 
        });
    }

    try {
        const [existingUser] = await connection.promise().query(
            'SELECT Uid FROM Users WHERE Email = ?',
            [email]
        );

        if (existingUser.length > 0) {
            return res.status(400).json({
                error: 'Email already in use', 
                message: 'Ez az email cím már használatban van.' 
            });
        }

        if (phoneNumber.trim().length !== 11) {
            return res.status(400).json({ 
                error: 'Phone number format invalid', 
                message: 'A telefonszám hossza nem megfelelő.' 
            });
        }

        const phoneRegex = /^06\d{9}$/;
        if (!phoneRegex.test(phoneNumber)) {
            return res.status(400).json({
                error: 'Phone number format invalid',
                message: 'A telefonszám formátuma érvénytelen. Helyes formátum például: 06123456789'
            });
        }

        // Hash the password
        const saltRounds = 10;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // Insert the user into the database
        const query = 'INSERT INTO Users (Fullname, Email, PassHashed, PhoneNumber) VALUES (?, ?, ?, ?)';
        await connection.promise().query(query, [fullname, email, hashedPassword, phoneNumber]);

        res.json({ message: 'Felhasználó sikeresen regisztrálva.' });

    } catch (error) {
        console.error('Error in /register:', error);
        res.status(500).json({ 
             error: err.message,
            message: userServerErrorMessage 
        });
    }
});




// Send Verification Email
app.post('/send-verify-email', async (req, res) => {
    try {
        const { email } = req.body;
        console.log('Received send-verify-email request:', email);

        if (!email || !validator.isEmail(email)) {
            return res.status(400).json({ 
                error: 'Invalid email address', 
                message: 'Érvénytelen vagy hiányzó email cím.' 
            });
        }

        // Check if user exists
        const [users] = await connection.promise().query(
            'SELECT Uid FROM Users WHERE Email = ?',
            [email]
        );
        if (users.length === 0) {
            return res.status(404).json({ 
                error: 'User not found', 
                message: userApiErrorMessage 
            });
        }

        const userId = users[0].Uid;

        // Generate token
        const code = await generateUniqueCode();

        // Insert into VerifyEmailRequests
        await connection.promise().query(
            'INSERT INTO VerifyEmailRequests (Uid, VerifyToken) VALUES (?, ?)',
            [userId, code]
        );

        // Send email
        const mailOptions = {
            from: 'filmbeadando2024@gmail.com',
            to: email,
            subject: 'Email verification - UsedPalace',
            text: `Your email verification code is: ${code}`
        };

        transporter.sendMail(mailOptions, (err) => {
            if (err) {
                console.error('Error sending email:', err);
                return res.status(500).json({ 
                    error: 'Failed to send email', 
                    message: 'Nem sikerült elküldeni az ellenőrző emailt. Próbáld újra későuserServerErrorMessage.' 
                });
            }
            res.json({ message: 'Az ellenőrző kód elküldve az email címedre.' });
        });

    } catch (err) {
        console.error('Error in /send-verify-email:', err);
        res.status(500).json({ 
             error: err.message,
            message: userServerErrorMessage 
        });
    }
});

// Verify Email
app.post('/verify-email', async (req, res) => {
    try {
        const { email, code } = req.body;
        console.log('Received verify-email request:', email);

        if (!email || !validator.isEmail(email)) {
            return res.status(400).json({ 
                error: 'Invalid email address', 
                message: 'Érvénytelen vagy hiányzó email cím.' 
            });
        }

        if (!code) {
            return res.status(400).json({
                error: 'Code missing',
                message: 'Az ellenőrző kód megadása kötelező.'
            });
        }

        // Find user + matching token
        const [results] = await connection.promise().query(`
            SELECT u.Uid 
            FROM Users u
            JOIN VerifyEmailRequests ver ON u.Uid = ver.Uid
            WHERE u.Email = ? AND ver.VerifyToken = ?`,
            [email, code]
        );

        if (results.length === 0) {
            return res.status(400).json({ 
                error: 'Invalid or expired verification code', 
                message: 'Érvénytelen vagy lejárt ellenőrző kód.' 
            });
        }

        const userId = results[0].Uid;

        // Update user verified status
        await connection.promise().query(
            'UPDATE Users SET Verified = TRUE WHERE Uid = ?',
            [userId]
        );

        // Delete the verification request
        await connection.promise().query(
            'DELETE FROM VerifyEmailRequests WHERE Uid = ?',
            [userId]
        );

        res.json({ message: 'Email cím sikeresen ellenőrizve.' });

    } catch (err) {
        console.error('Error in /verify-email:', err);
        res.status(500).json({ 
             error: err.message,
            message: userServerErrorMessage 
        });
    }
});


app.post('/get-safe-user-data', authenticateToken , async (req, res) => {
    try {
        const { userID } = req.body;

        if (!userID) {
            return res.status(400).json({
                success: false,
                error: "Search parameter is missing",
                message: "Töltsd ki a keresési mezőt.",
                data: null
            });
        }

        const query = 'SELECT Email, Fullname, PhoneNumber FROM Users WHERE Uid = ?';
        const [results] = await connection.promise().query(query, [userID]);

        if (results.length === 0) {
            return res.json({
                success: false,
                error: "There are no users with this ID",
                message:  userApiErrorMessage,
                data: null
            });
        }

        const user = results[0];

        res.json({
            success: true,
            data: {
                email: user.Email,
                fullname: user.Fullname,
                phoneNumber: user.PhoneNumber
            }
        });

    } catch (err) {
        console.error('Error in /get-safe-user-data:', err);
        res.status(500).json({
            success: false,
            message: userServerErrorMessage,
			error: err.message,
            data: null 
        });
    }
});





// Endpoints for sales
// Fetch sales data
app.get('/getSales', authenticateToken, async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;   // alapértelmezett: 1. oldal
        const limit = parseInt(req.query.limit) || 10; // alapértelmezett: 10 elem
        const offset = (page - 1) * limit;
		console.log('get sales:', page, limit);
        // Összes rekord megszámolása (oldalszámhoz)
        const [[{ total }]] = await connection.promise().query(
            'SELECT COUNT(*) AS total FROM Sales'
        );

        // Csak adott oldal lekérdezése
        const [results] = await connection.promise().query(
            'SELECT * FROM Sales LIMIT ? OFFSET ?',
            [limit, offset]
        );

        if (results.length === 0) {
            return res.status(404).json({
                error: 'No sales found',
                message: 'Nincs elérhető értékesítési adat.'
            });
        }

        res.json({
            success: true,
            currentPage: page,
            totalPages: Math.ceil(total / limit),
            totalItems: total,
            data: results
        });

    } catch (err) {
        console.error('Error in /getSales:', err);
        res.status(500).json({
            message: userServerErrorMessage
        });
    }
});




function getSaleImages(saleFolder) {
    const folderPath = path.join(__dirname, 'sales', saleFolder); // a mappa elérési útja
    if (!fs.existsSync(folderPath)) return [];

    return fs.readdirSync(folderPath) // listázza a fájlokat
        .filter(file => file.endsWith('.jpg')); // csak jpg képeket
}

app.post('/search-sales-withSID', authenticateToken , async (req, res) => {
    try {
        const { searchParam } = req.body;

        if (!searchParam) {
            return res.status(400).json({
                success: false,
				error: "Search parameter is missing",
                message: "Töltsd ki a keresési mezőt.",
                data: null
            });
        }

        const query = 'SELECT * FROM Sales WHERE Sid = ?';
        const [results] = await connection.promise().query(query, [searchParam]);

        if (results.length === 0) {
            return res.json({
                success: true,
                message: userApiErrorMessage,
				error:"No product found with this ID",
                data: null
            });
        }

        const sale = results[0];
        const images = getSaleImages(sale.SaleFolder);

        res.json({
            success: true,
            message: "Találat",
            data: {
                ...sale,
                Images: images // itt adjuk hozzá a képek listáját
            }
        });

    } catch (err) {
        console.error('Error in /search-sales-withSID:', err);
        res.status(500).json({
            success: false,
			message: userApiErrorMessage,
            data: null 
        });
    }
});


app.post('/search-deletedSales-withSID', authenticateToken , async (req, res) => {
    try {
		
        const { searchParam } = req.body;
        console.log('Received deleted sales search request:', searchParam);
		
        if (!searchParam) {
            return res.status(400).json({
                success: false,
                error: "Search parameter is missing",
                message: "Töltsd ki a keresési mezőt.",
                data: null
            });
        }

        const query = 'SELECT * FROM DeletedSales WHERE Sid = ?';
        
        const [results] = await connection.promise().query(query, [searchParam]);

        if (results.length === 0) {
            return res.json({
                success: true,
                message: userApiErrorMessage,
				error:"No product found with this ID",
                data: null 
            });
        }

 
        res.json({
            success: true,
            data: results[0]  
        });
        
    } catch (err) {
        console.error('Error in /search-deletedSales-withSID:', err);
        res.status(500).json({
            success: false,
            
			message: userApiErrorMessage,
            data: null 
        });
    }
});

app.post('/search-sales-withSaleName', authenticateToken , async (req, res) => {
    try {
        const { searchParam } = req.body;
        console.log('Received search request:', searchParam);

        if (!searchParam) {
            return res.status(400).json({
                success: false,
                error: "Search parameter is missing",
                message: "Töltsd ki a keresési mezőt.",
                data: []
            });
        }

        const query = 'SELECT * FROM Sales WHERE Name LIKE ?';
        const searchValue = `%${searchParam}%`;

        const [results] = await connection.promise().query(query, [searchValue]);

        // Ensure we always return the same structure
        res.json({
            success: true,
			message: results.length ? "Találat" : "Nincs találat",
            error: results.length ? "Products found" : "No products found",
            data: results
        });
        
    } catch (err) {
        console.error('Error in /search-sales:', err);
        res.status(500).json({
            success: false,
            
			message: userApiErrorMessage,
            data: []
        });
    }
});

app.post('/search-sales-withCategory', authenticateToken , async (req, res) => {
    try {
        const { searchParam } = req.body;
        console.log('Received search request:', searchParam);

        if (!searchParam) {
            return res.status(400).json({
                success: false,
                error: "Search parameter is missing",
                message: "Töltsd ki a keresési mezőt.",
                data: []
            });
        }

        const query = 'SELECT * FROM Sales WHERE BigCategory LIKE ? OR SmallCategory LIKE ?';
        const searchValue = `%${searchParam}%`;

        const [results] = await connection.promise().query(query, [searchValue, searchValue]);

        // Ensure we always return the same structure
        res.json({
            success: true,
			message: results.length ? "Találat" : "Nincs találat",
            error: results.length ? "Products found" : "No products found",
            data: results
        });
        
    } catch (err) {
        console.error('Error in /search-sales:', err);
        res.status(500).json({
            success: false,
            
			message: userApiErrorMessage,
            data: []
        });
    }
});

app.post('/search-salesID', authenticateToken , async (req, res) => {
    try {
        const { searchParam } = req.body;
        console.log('Received search request:', searchParam);

        if (!searchParam) {
            return res.status(400).json({
                success: false,
                message: "Töltsd ki a keresési mezőt.",
                error: "Search parameter is missing",
                data: []
            });
        }

        const query = 'SELECT * FROM Sales WHERE Uid = ?';
        const [results] = await connection.promise().query(query, [searchParam]);

        if (results.length === 0) {
            return res.status(404).json({
                success: true,
                message: userApiErrorMessage,
                error: "No product found with this ID",
                data: []
            });
        }

        res.json({
            success: true,
            data: results
        });
        
    } catch (err) {
        console.error('Error in /search-salesID:', err);
        res.status(500).json({
            success: false,
            message: userApiErrorMessage,
            error: err.message,
            data: []
        });
    }
});



app.post('/create-sale', authenticateToken , (req, res) =>  {
    try {
        const { name, description, cost, bigCategory, smallCategory, userId } = req.body;


        if (!name || !description || !cost || !bigCategory || !userId) {
            return res.status(400).json({
                success: false,
                message: "Kérjük minden mezőt töltsön ki!",
				error:"Missing required fields"
            });
        }
		console.log('Received sale creation request: ', userId);

        // Create unique folder name
        const saleFolder = `sale_${uuidv4()}`;
        const saleFolderPath = path.join('sales', saleFolder);

        // Create the folder
        if (!fs.existsSync(saleFolderPath)) {
            fs.mkdirSync(saleFolderPath, { recursive: true });
        }

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
						error:'Failed to create sale',
                        message: 'Nem sikerült létrehozni a hirdetést.' 
                    });
                }
                
                res.json({ 
                    success: true,
                    message: 'Hirdetés sikeresen létrehozva!',
                    saleId: results.insertId,
                    saleFolder: saleFolder
                });
            }
        );

    } catch (err) {
        console.error('Error in /create-sale:', err);
        res.status(500).json({ 
            success: false,
            message: userApiErrorMessage,
			error: err.message
        });
    }
});

//modify sale endpoint
app.put('/modify-sale', authenticateToken , async (req, res) => {
    try {
        const { saleId, name, description, cost, bigCategory, smallCategory, userId } = req.body;
        
		console.log('Received sale modification request: ', userId);
		
       
        if (!saleId || !name || !description || !cost || !bigCategory || !userId) {
            return res.status(400).json({
                success: false,
                message: "Kérjük minden mezőt töltsön ki!",
				error:"Missing required fields"
            });
        }

        const [sale] = await connection.promise().query(
            'SELECT Uid, SaleFolder FROM Sales WHERE Sid = ?',
            [saleId]
        );
        
        if (sale.length === 0) {
            return res.status(404).json({
                success: false,
                message: userApiErrorMessage,
				error: "Sale not found"
            });
        }

        if (sale[0].Uid !== userId) {
            return res.status(403).json({
                success: false,
				message: userApiErrorMessage,
                error: "Unauthorized"
            });
        }

        const updateQuery = `
            UPDATE Sales 
			SET Name = ?,Description = ?, Cost = ?, BigCategory = ?, SmallCategory = ?
			WHERE Sid = ?`;
        await connection.promise().query(
            updateQuery, 
            [name, description, cost, bigCategory, smallCategory, saleId]
        );

        res.json({
            success: true,
            message: "Sale modified successfully",
            saleId: saleId,
            saleFolder: sale[0].SaleFolder
        });

    } catch (err) {
        console.error('Error in /modify-sale:', err);
        res.status(500).json({ 
            success: false,
            message: userApiErrorMessage,
            error: err.message
        });
    }
});	

//Delete sale end point
app.delete('/delete-sale', authenticateToken , async (req, res) => {
    try {
        const { saleId, userId } = req.body;
		console.log('Received sale deletion request: ', userId);
		
        if (!saleId || !userId) {
            return res.status(400).json({
                success: false,
                message: "Kérjük töltsön ki minden mezőt.",
				error: "Both saleId and userId are required",
                data: null
            });
        }

        const [sale] = await connection.promise().query(
            'SELECT Uid FROM Sales WHERE Sid = ?', 
            [saleId]
        );

        if (sale.length === 0) {
            return res.status(404).json({
                success: false,
				message: userApiErrorMessage,
                error: "Sale not found",
                data: null
            });
        }

        if (sale[0].Uid !== userId) {
            return res.status(403).json({
                success: false,
				message: userApiErrorMessage,
                error: "Unauthorized - you can only delete your own sales",
                data: null
            });
        }

        // Get sale folder before deletion
        const [saleData] = await connection.promise().query(
            'SELECT SaleFolder FROM Sales WHERE Sid = ?',
            [saleId]
        );

		await connection.promise().query(
			'INSERT INTO DeletedSales (Sid, Uid) SELECT Sid, Uid FROM Sales WHERE Uid = ?',
			[userId]
		);
		
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
            message: "Hirdetés sikeresen törölve",
            data: {
                deletedSaleId: saleId
            }
        });

    } catch (err) {
        console.error('Error in /delete-sale:', err);
        res.status(500).json({ 
            success: false,
            message: userApiErrorMessage,
            error: err.message
        });
    }
});




//Image Uploader and stuff for it

// Delete multiple images by filenames
app.post('/delete-images', authenticateToken, async (req, res) => {
    try {
		
        const { saleFolder, imageNames } = req.body;
		console.log('Image deletion in this folder: ', saleFolder);

        if (!saleFolder || !Array.isArray(imageNames) || imageNames.length === 0) {
            return res.status(400).json({
                success: false,
                message: userApiErrorMessage,
				error: "saleFolder and non-empty imageNames array are required"
            });
        }

        const deletedImages = [];

        imageNames.forEach(name => {
            // Biztonsági okokból tisztítsuk a fájlnevet, hogy ne lehessen könyvtárat átugrani
            const safeName = path.basename(name);
            const imagePath = path.join('sales', saleFolder, safeName);
            if (fs.existsSync(imagePath)) {
                fs.unlinkSync(imagePath);
                deletedImages.push(safeName);
            }
        });

        return res.json({
            success: true,
            message: "Sikeres törlés",
            deletedImages
        });

    } catch (err) {
        console.error('Error in /delete-images:', err);
        res.status(500).json({
            success: false,
            message: userServerErrorMessage,
            error: err.message
        });
    }
});



//Get images for modify
app.post('/get-images-with-saleId', authenticateToken , async (req, res) => {
    try {
        const { searchParam } = req.body;
        
        if (!searchParam) {
            return res.status(400).json({
                success: false,
                message: "Kérjük töltsön ki minden mezőt",
				error: "Search parameter is required",
                data: null
            });
        }

        const query = 'SELECT * FROM Sales WHERE Sid = ? LIMIT 1';
        const [results] = await connection.promise().query(query, [searchParam]);

        res.json({
            success: true,
            message: results.length ? "Hirdetés megtalálva" : "Nem található hirdetés",
			error: results.length ? "Product found" : "No product found",
            data: results[0] || null // Return single object or null
        });
        
    } catch (err) {
        console.error('Error in /search-sales:', err);
        res.status(500).json({
            success: false,
            message: userServerErrorMessage,
            data: null
        });
    }
});

// Dinamikus tárolási útvonal
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        const saleFolder = req.body.saleFolder;
        const folderPath = path.join('sales', saleFolder);
        if (!fs.existsSync(folderPath)) {
            return cb(new Error('Sale folder does not exist'), null);
        }
        cb(null, folderPath);
    },
    filename: (req, file, cb) => {
        cb(null, Date.now() + '-' + file.originalname);
    }
});

const upload = multer({ storage: storage });

app.post('/upload-sale-images', authenticateToken, upload.array('images', 10), (req, res) => {
    const files = req.files.map(file => file.filename);
    res.json({
        success: true,
        message: 'Képek sikeresen feltöltve',
        files: files
    });
});



app.post('/get-sale-images', async (req, res) => {
	const { sid } = req.body;

    const [results] = await connection.promise().query(
        'SELECT SaleFolder FROM Sales WHERE Sid = ?', [sid]
    );

    if (results.length === 0) {
        return res.status(404).json({ 
		success: false, 
		message: userApiErrorMessage,
		error:	'Sale not found'	});
    }

    const saleFolder = results[0].SaleFolder;
    const folderPath = path.join('sales', saleFolder);

    if (!fs.existsSync(folderPath)) {
        return res.json({ success: true, images: [] });
    }

    const files = fs.readdirSync(folderPath);
    const imageUrls = files.map(file => `${req.protocol}://${req.get('host')}/sales/${saleFolder}/${file}`);
	//const imageUrls = files.map(file => `http://10.224.83.75:3000/sales/${saleFolder}/${file}`);

    res.json({ 
	success: true, 
	images: imageUrls });
});



// Error handling middleware for multer
app.use((err, req, res, next) => {
    if (err instanceof multer.MulterError) {
        // A Multer error occurred when uploading
        return res.status(400).json({
            success: false,
			error: 'Multer error while uploading',
            message: 'Hiba történt a képek feltöltése közben'
        });
    } else if (err) {
        // An unknown error occurred
        return res.status(500).json({
            success: false,
			
            message: userApiErrorMessage
        });
    }
    next();
});



















//For the chat part
// Get or create chat between users for a specific sale
app.post('/initiate-chat', authenticateToken , async (req, res) => {
    try {
        const { sellerId, buyerId, saleId } = req.body;
		console.log('Received initiate chat request for sale: ', saleId);

        // Input validation
        if (!sellerId || !buyerId || !saleId) {
            return res.status(400).json({
                success: false,
                error: "Missing required fields (sellerId, buyerId, saleId)",
				message: "Kérjük töltsön ki minden mezőt."
            });
        }
		
		   
		// Input validation
        if (  !validator.isInt(String(sellerId)) || !validator.isInt(String(buyerId)) || !validator.isInt(String(saleId)) ) {
				return res.status(400).json({
					success: false,
					message: userApiErrorMessage,
					error: "Sending request error"
				});
		}

        // Check if chat already exists
        const [existingChat] = await connection.promise().query(
            'SELECT ChatID FROM Chats WHERE SellerID = ? AND BuyerID = ? AND SaleID = ?',
            [sellerId, buyerId, saleId]
        );

        if (existingChat.length > 0) {
            return res.json({ 
                success: true, 
                chatId: existingChat[0].ChatID,
                isNew: false,
                message: userApiErrorMessage,
				error: "Chat already exists"
            });
        }

        const [result] = await connection.promise().query(
            'INSERT INTO Chats (SellerID, BuyerID, SaleID) VALUES (?, ?, ?)',
            [sellerId, buyerId, saleId]
        );

        await connection.promise().query(
            'UPDATE Chats SET LastMessageAt = CURRENT_TIMESTAMP WHERE ChatID = ?',
            [result.insertId]
        );

        res.json({ 
            success: true, 
            chatId: result.insertId,
            isNew: true,
            message: "Chat sikeresen létrehozva"
        });

    } catch (err) {
        console.error('Error initiating chat:', err);
        res.status(500).json({ 
            success: false, 
			message: userApiErrorMessage,
            error: err.error
        });
    }
});

//Load all chats for user
// Load all chats for user with unread message count
app.post('/load-user-chats', authenticateToken, async (req, res) => {
    try {
        const { userId } = req.body;

        if (!userId) {
            return res.status(400).json({
                success: false,
                message: "User ID required",
                error: "User ID required",
                data: []
            });
        }

        // Lekérdezzük a chat-eket és az olvasatlan üzenetek számát
        const query = `
			SELECT 
				c.ChatID,
				c.SellerID,
				c.BuyerID,
				c.SaleID,
				c.CreatedAt,
				c.LastMessageAt,
				SUM(CASE WHEN m.SenderID != ? AND m.isRead = 0 THEN 1 ELSE 0 END) AS unreadCount
			FROM Chats c
			LEFT JOIN Messages m ON c.ChatID = m.ChatID
			WHERE c.BuyerID = ? OR c.SellerID = ?
			GROUP BY c.ChatID, c.SellerID, c.BuyerID, c.SaleID, c.CreatedAt, c.LastMessageAt
			ORDER BY c.LastMessageAt DESC
		`;


        const [results] = await connection.promise().query(query, [userId, userId, userId]);
        res.json({
            success: true,
            message: results.length ? "Chat-ek találhatóak" : "Nem található chat",
            error: results.length ? "" : "No chats found",
            data: results
        });

    } catch (err) {
        console.error('Error loading chats:', err);
        res.status(500).json({
            success: false,
            message: "Hiba történt a chat-ek betöltésekor",
            data: []
        });
    }
});


//Search Username
app.post('/search-username', authenticateToken , async (req, res) => {
    try {
        const { searchParam } = req.body;
           console.log('Received search username request: ', searchParam);
		
        if (!searchParam) {
            return res.status(400).json({
                success: false,
                error: "Search parameter is required",
				message: userApiErrorMessage,
                data: null
            });
        }

        const query = 'SELECT Fullname FROM Users WHERE Uid = ? LIMIT 1';
        const [results] = await connection.promise().query(query, [searchParam]);

        if (results.length === 0) {
			
			const query2 = 'SELECT Id FROM DeletedUsers WHERE Uid = ? LIMIT 1';
			const [results2] = await connection.promise().query(query2, [searchParam]);
			//Keresni a deleted usersben
			if (results2.length === 0) {
            return res.status(404).json({
                success: false,
                message: userApiErrorMessage,
				error: "User not found",
                data: null
            });
			}
			
			return res.json({
            success: true,
            message: "Sikeres törlés",
            Fullname: "Deleted User"
           	});

        } 

        res.json({
            success: true,
            Fullname: results[0].Fullname
           
        });

    } catch (err) {
        console.error('Error searching username:', err);
        res.status(500).json({ 
            success: false, 
			message: userApiErrorMessage,
            
            data: null
        });
    }
});

//WEBSOCKET SEND AND GET MESSAGE

const wss = new WebSocket.Server({ port: 8080 });

const chatClients = {}; // { chatId: [ws1, ws2] }

wss.on('connection', (ws) => {
  ws.on('message', async (msg) => {
    let data;
    try {
      data = JSON.parse(msg);
    } catch {
      ws.send(JSON.stringify({ type: 'error', message: 'Invalid JSON' }));
      return;
    }

    if (data.type === 'join-chat') {
      const chatId = data.chatId;
      if (!chatClients[chatId]) chatClients[chatId] = [];
      chatClients[chatId].push(ws);
      ws.chatId = chatId;
      ws.send(JSON.stringify({ type: 'info', message: `Joined chat ${chatId}` }));
      return;
    }

    if (data.type === 'send-message') {
      const { chatId, senderId, content } = data;
      if (!chatId || !senderId || !content) {
        ws.send(JSON.stringify({ type: 'error', message: 'Missing fields' }));
        return;
      }

      try {
        const [result] = await connection.promise().query(
		  'INSERT INTO Messages (ChatID, SenderID, Content, isRead) VALUES (?, ?, ?, 0)',
		  [chatId, senderId, content]
		);

        await connection.promise().query(
          'UPDATE Chats SET LastMessageAt = CURRENT_TIMESTAMP WHERE ChatID = ?',
          [chatId]
        );

        const [messageRow] = await connection.promise().query(
          'SELECT MessageID, SentAt FROM Messages WHERE MessageID = ? LIMIT 1',
          [result.insertId]
        );

        const outgoingMsg = {
          type: 'new-message',
          chatId,
          messageId: result.insertId,
          senderId,
          content,
          sentAt: messageRow[0].SentAt
        };

        // Küldd el az összes kliensnek, akik csatlakoztak ehhez a chathez
        (chatClients[chatId] || []).forEach(clientWs => {
          if (clientWs.readyState === WebSocket.OPEN) {
            clientWs.send(JSON.stringify(outgoingMsg));
          }
        });
		
		// --- PUSH ÉRTESÍTÉS ---
		// Lekérjük a chat résztvevőit (pl. UserFcmTokens táblából)
		const [tokens] = await connection.promise().query(
		  `SELECT FcmToken,username 
		   FROM UserFcmTokens 
		   WHERE UserID != ? 
			 AND UserID IN (
				 SELECT DISTINCT SenderID 
				 FROM Messages 
				 WHERE ChatID = ?
			 )`,
		  [senderId, chatId]
		);

		for (const row of tokens) {
			console.log('row of token:', row.FcmToken);
			await sendChatNotification(row.FcmToken, chatId, row.username, content);
		}
		
		
      } catch (err) {
        ws.send(JSON.stringify({ type: 'error', message: 'DB error: ' + err.message }));
      }
      return;
    }

    if (data.type === 'get-messages') {
      const chatId = data.chatId;
      if (!chatId) {
        ws.send(JSON.stringify({ type: 'error', message: 'chatId is required' }));
        return;
      }
      try {
        const [messages] = await connection.promise().query(
          'SELECT * FROM Messages WHERE ChatID = ? ORDER BY SentAt ASC',
          [chatId]
        );
        ws.send(JSON.stringify({ type: 'chat-messages', chatId, messages }));
      } catch (err) {
        ws.send(JSON.stringify({ type: 'error', message: 'DB error: ' + err.message }));
      }
      return;
    }
	
	if (data.type === 'mark-as-read') {
		const { chatId, readerId } = data;
		try {
			await connection.promise().query(
				'UPDATE Messages SET isRead = 1 WHERE ChatID = ? AND SenderID != ?',
				[chatId, readerId]
			);
		} catch (err) {
			console.error('Failed to mark messages as read:', err);
		}
	}


    ws.send(JSON.stringify({ type: 'error', message: 'Unknown message type' }));
  });

  ws.on('close', () => {
    // Töröld a ws-t a chatClients listából
    if (ws.chatId && chatClients[ws.chatId]) {
      chatClients[ws.chatId] = chatClients[ws.chatId].filter(c => c !== ws);
      if (chatClients[ws.chatId].length === 0) {
        delete chatClients[ws.chatId];
      }
    }
  });
});



admin.initializeApp({
    credential: admin.credential.cert(require('./serviceAccountKey.json'))
});

async function sendChatNotification(fcmToken, chatId, senderName, content) {
    const message = {
		token: fcmToken,
		data: { chatId: chatId.toString(), senderName, content },
		notification: { title: senderName, body: content },
	};


    try {
        await admin.messaging().send(message);
        console.log('Notification sent');
    } catch (err) {
        console.error('Error sending notification:', err);
    }
}

app.post('/save-fcm-token', authenticateToken, async (req, res) => {
    const { userId, fcmToken } = req.body;
    if (!userId || !fcmToken) return res.status(400).json({ success: false });

    try {
		
		const [userRows] = await connection.promise().query(
            'SELECT Fullname FROM Users WHERE Uid = ?',
            [userId]
        );

        if (userRows.length === 0) return res.status(404).json({ success: false, message: 'User not found' });

        const username = userRows[0].Fullname;
        // Ellenőrizd, hogy már létezik-e a token
        const [existing] = await connection.promise().query(
            'SELECT * FROM UserFcmTokens WHERE UserID = ? AND FcmToken = ?',
            [userId, fcmToken]
        );

        if (existing.length === 0) {
            await connection.promise().query(
                'INSERT INTO UserFcmTokens (UserID, FcmToken, username) VALUES (?, ?, ?)',
                [userId, fcmToken, username]
            );
        } else {
            await connection.promise().query(
                'UPDATE UserFcmTokens SET UpdatedAt = CURRENT_TIMESTAMP WHERE UserID = ? AND FcmToken = ?',
                [userId, fcmToken]
            );
        }

        res.json({ success: true });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false });
    }
});















//PROFILban modifyok és delete
//modify phonenumber
app.put('/modify-user-phone', authenticateToken , async (req, res) => {
	try{
		const { phoneNumber, userId } = req.body //06204445566
		
		if (!userId) {
			return res.status(400).json({
				success: false,
				message: userApiErrorMessage,
				error: "Missing userId"
			});
		}
		
		
		const [userResult] = await connection.promise.query(
			  "SELECT * FROM Users WHERE Uid = ?",
			  [userId]
			);

			if (userResult.length === 0) {
			  return res.status(404).json({
				success: false,
				message: userApiErrorMessage
			  });
			}
		
		if ( !phoneNumber) {
			 return res.status(400).json({
                success: false,
                message: "Missing required fields"
            });
		}
		
		if (phoneNumber.trim().length != 11) {
			return res.status(400).json({ error: 'Phone number format invalid.' });
		}
		
		const phoneRegex = /^06\d{9}$/;
		if (!phoneRegex.test(phoneNumber)) {
				return res.status(400).json({
				success: false,
				message: "Phone number format invalid"
				});
		}
		
		const currentPhoneNumber = userResult[0].PhoneNumber;
		if (currentPhoneNumber === phoneNumber) {
			return res.status(400).json({
				success: false,
				message: "The new phone number cannot be the same as the current one"
			});
		}
		
		const updateQuery = `
			UPDATE Users
			SET PhoneNumber = ?
			WHERE Uid = ?`;
		await connection.promise.query(
			updateQuery,
			[phoneNumber, userId]
		);
		
		  res.json({
            success: true,
            message: "User modified successfully",
        });
		
		
	} catch (err) {
		 console.error('Error modifying user account: ', err)
		         res.status(500).json({ 
            success: false,
            message: userApiErrorMessage,
            error: err.message
        });
	}
});


//modify Email
app.post('/request-user-email-change', authenticateToken , async (req, res) => {  //create request és send email
    try {

        const { userId, newEmail } = req.body;
		console.log('Received email change request from:', userId);
		
        if (!userId || !newEmail) {
            return res.status(400).json({
                success: false,
                message: 'Missing required data'
            });
        }
		
		if (!validator.isEmail(newEmail.trim())) {
			return res.status(400).json({
                success: false,
                message: 'Invalid email address.'
            });
		}
		
		const [existingUser] = await connection.promise().query(
            'SELECT Uid FROM Users WHERE Email = ?',
            [newEmail]
        );

        if (existingUser.length > 0) {
            return res.status(400).json({
                success: false,
                message: 'Email address already in use.'
            });
        }
	

        const code = await generateUniqueCode();

        const insertQuery = `
            INSERT INTO EmailChangeRequests (Uid, NewEmail, VerifyToken)
            VALUES (?, ?, ?)
        `;
        await connection.promise().query(insertQuery, [userId, newEmail, code]);

        const mailOptions = {
            from: 'filmbeadando2024@gmail.com',
            to: newEmail,
            subject: 'Confirm your new email address',
            text: `Your email verification code is: ${code}`
        };

        transporter.sendMail(mailOptions, (err, info) => {
            if (err) {
                console.error('Error sending email:', err);
                return res.status(500).json({
                    success: false,
                    message: 'Failed to send verification email'
                });
            }

            res.json({
                success: true,
                message: 'Verification email sent to new address'
            });
        });

    } catch (err) {
        console.error('Error in /request-user-email-change:', err);
        res.status(500).json({
            success: false,
            message: userApiErrorMessage,
            error: err.message
        });
    }
});


app.put('/confirm-user-email-change', authenticateToken , async (req, res) => {
    try {
        const { userId, code } = req.body;
		
		console.log('Received conformation request for email change from:', userId);
		
        if (!userId || !code) {
            return res.status(400).json({
                success: false,
                message: 'Missing required fields'
            });
        }

        const [rows] = await connection.promise().query(
            'SELECT NewEmail, VerifyToken, CreatedAt FROM EmailChangeRequests WHERE Uid = ? ORDER BY CreatedAt DESC LIMIT 1',
            [userId]
        );

        if (rows.length === 0) {
            return res.status(404).json({
                success: false,
                message: userApiErrorMessage
            });
        }

        const request = rows[0];
		
		const createdAt = new Date(request.CreatedAt);
		const now = new Date();
		const diffMinutes = (now - createdAt) / 100 / 60; //ez 10 perc

		if (diffMinutes > 10) {
			return res.status(400).json({ message: 'Verification code expired' });
		}

        if (request.VerifyToken !== code) {
            return res.status(400).json({
                success: false,
                message: 'Invalid verification code'
            });
        }
		
		const [activeSessions] = await connection.promise().query(
			`SELECT COUNT(*) AS count FROM sessions WHERE UserId = ? AND ExpiresAt > NOW()`,
			[userId]
		);

		if (activeSessions[0].count > 1) {
			return res.status(400).json({
				message: userApiErrorMessage,
				error: 'More than 1 active session.'
			});
		}

        await connection.promise().query(
            'UPDATE Users SET Email = ? WHERE Uid = ?',
            [request.NewEmail, userId]
        );

        await connection.promise().query(
            'DELETE FROM EmailChangeRequests WHERE Uid = ?',
            [userId]
        );

        res.json({
            success: true,
            message: 'Email address updated successfully'
        });

    } catch (err) {
        console.error('Error in /confirm-user-email-change:', err);
        res.status(500).json({
            success: false,
            message: userApiErrorMessage,
            error: err.message
        });
    }
});



//modify password
app.post('/request-password-change', authenticateToken , async (req, res) => {
    try {
        const { userId, oldPassword, newPassword } = req.body;
		
		  console.log('Received password change request from:', userId);
		
        if (!userId || !oldPassword || !newPassword) {
            return res.status(400).json({ message: 'Missing fields' });
        }

        const [rows] = await connection.promise().query(
            'SELECT PassHashed, Email FROM Users WHERE Uid = ?', [userId]
        );

        if (rows.length === 0) {
            return res.status(404).json({ message: 'User not found' });
        }

        const user = rows[0];
		
		//Old password cant match newPassword
        if (oldPassword === newPassword) {
            return res.status(401).json({ message: 'Old password cant match new password' });
        }
		
		//Password validation
        const passwordMatch = await bcrypt.compare(oldPassword, user.PassHashed);
        if (!passwordMatch) {
            return res.status(401).json({ message: 'Old password is incorrect' });
        }
		
		//New password validation
		
		if (newPassword.trim().length < 8) {
			return res.status(400).json({ error: 'Password must be atleast 8 characters.' });
		}
		
		const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
			if (!passwordRegex.test(newPassword)) {
				return res.status(400).json({ message: 'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character.' });
		}

        const code = await generateUniqueCode();

        const hashedNewPassword = await bcrypt.hash(newPassword, 10);

        const insertQuery = `
            INSERT INTO PasswordChangeRequests (Uid, NewPassword, VerifyToken)
            VALUES (?, ?, ?)
        `;
        await connection.promise().query(insertQuery, [userId, hashedNewPassword, code]);

        const mailOptions = {
            from: 'filmbeadando2024@gmail.com',
            to: user.Email,
            subject: 'Confirm your password change',
            text: `Your password change verification code is: ${code}`
        };

        transporter.sendMail(mailOptions, (err) => {
            if (err) {
                console.error(err);
                return res.status(500).json({ message: 'Failed to send email' });
            }
            res.json({ message: 'Verification code sent' });
        });

    } catch (err) {
        console.error('Error in request-password-change:', err);
        res.status(500).json({ 
			success: false,
            message: userApiErrorMessage,
            error: err.message
		});
    }
});



app.put('/confirm-password-change', authenticateToken , async (req, res) => {
    try {
		
        const { userId, code } = req.body;
		    console.log('Received conformation request for password change from:', userId);
		
        const [rows] = await connection.promise().query(
            'SELECT NewPassword, VerifyToken, CreatedAt FROM PasswordChangeRequests WHERE Uid = ? ORDER BY CreatedAt DESC LIMIT 1',
            [userId]
        );

        if (rows.length === 0) {
            return res.status(404).json({ message: 'No pending password change found' });
        }

        const request = rows[0];
		
		const createdAt = new Date(request.CreatedAt);
		const now = new Date();
		const diffMinutes = (now - createdAt) / 100 / 60; //ez 10 perc

		if (diffMinutes > 10) {
			return res.status(400).json({ message: 'Verification code expired' });
		}
        if (request.VerifyToken !== code) {
            return res.status(400).json({ message: 'Invalid verification code' });
        }
		
		const [activeSessions] = await connection.promise().query(
			`SELECT COUNT(*) AS count FROM sessions WHERE UserId = ? AND ExpiresAt > NOW()`,
			[userId]
		);

		if (activeSessions[0].count > 1) {
			return res.status(400).json({
				error: 'More than 1 active session.'
			});
		}

        //Use the stored hashed password directly
        await connection.promise().query(
            'UPDATE Users SET PassHashed  = ? WHERE Uid = ?',
            [request.NewPassword, userId]
        );

        await connection.promise().query(
            'DELETE FROM PasswordChangeRequests WHERE Uid = ?',
            [userId]
        );

        res.json({ message: 'Password changed successfully' });

    } catch (err) {
        console.error('Error in confirm-password-change:', err);
        res.status(500).json({ 
			success: false,
            message: userApiErrorMessage,
            error: err.message
			});
    }
});



app.post('/request-phoneNumber-change', authenticateToken , async (req, res) => {
    try {
        const { userId, password } = req.body;

        console.log('Received phone number change request from:', userId);

        if (!userId || !password) {
            return res.status(400).json({ message: 'Missing fields' });
        }

        const query = 'SELECT * FROM Users WHERE Uid = ?';
        const [results] = await connection.promise().query(query, [userId]);

        if (results.length === 0) {
            return res.status(404).json({ message: userApiErrorMessage });
        }

        const user = results[0];

        const isPasswordValid = await bcrypt.compare(password, user.PassHashed);
        if (!isPasswordValid) {
            return res.status(401).json({ message: 'Invalid password' });
        }

        const mailOptions = {
            from: 'filmbeadando2024@gmail.com',
            to: user.Email,
            subject: 'Phone number change requested',
            text: `A phone number change has been requested for your account. If this wasn't you, please contact support immediately.`
        };

        transporter.sendMail(mailOptions, (err) => {
            if (err) {
                console.error(err);
                return res.status(500).json({ message: 'Failed to send email' });
            }
            res.json({ message: 'Password valid, you can now enter new phone number' });
        });

    } catch (err) {
        console.error('Error in request-phoneNumber-change:', err);
        res.status(500).json({ 
			success: false,
            message: userApiErrorMessage,
            error: err.message
		});
    }
});

app.put('/confirm-phoneNumber-change', authenticateToken , async (req, res) => {
    try {
        const { userId, phoneNumber } = req.body;
        console.log('Received confirmation request for phone number change from:', userId);

        if (!userId || !phoneNumber) {
            return res.status(400).json({ message: 'Missing fields' });
        }
		
		if (phoneNumber.trim().length != 11) {
			return res.status(400).json({ message: 'Telefonszám formátuma nem megfelelő' });
		}
	
		const phoneRegex = /^06\d{9}$/;
		if (!phoneRegex.test(phoneNumber)) {
			return res.status(400).json({
				message: 'Telefonszám formátuma nem megfelelő'
			});
		}
		
		const [activeSessions] = await connection.promise().query(
			`SELECT COUNT(*) AS count FROM sessions WHERE UserId = ? AND ExpiresAt > NOW()`,
			[userId]
		);

		if (activeSessions[0].count > 1) {
			return res.status(400).json({
				error: 'More than 1 active session.'
			});
		}


        const [rows] = await connection.promise().query(
            'SELECT PhoneNumber FROM Users WHERE Uid = ?',
            [userId]
        );

        if (rows.length === 0) {
            return res.status(404).json({ message: 'User not found' });
        }

        const currentPhoneNumber = rows[0].PhoneNumber;

        if (currentPhoneNumber === phoneNumber) {
            return res.status(400).json({ message: 'New phone number cannot be the same as the current one' });
        }

        await connection.promise().query(
            'UPDATE Users SET PhoneNumber = ? WHERE Uid = ?',
            [phoneNumber, userId]
        );

        res.json({ message: 'Phone number changed successfully' });

    } catch (err) {
        console.error('Error in confirm-phoneNumber-change:', err);
        res.status(500).json({ 
			success: false,
            message: userApiErrorMessage,
            error: err.message
		});
    }
});




//delete account

app.post('/request-delete-user', authenticateToken , async (req, res) => {
	try {
		const { userId } = req.body;
		console.log('Received deletion request for user: ', userId);
		 
		const query = 'SELECT * FROM Users WHERE Uid = ?';
        const [results] = await connection.promise().query(query, [userId]);

        if (results.length === 0) {
            return res.status(404).json({ message: userApiErrorMessage });
        }
	
        if (!userId) {
            return res.status(400).json({
                success: false,
				message: userApiErrorMessage,
                error: "UserId are missing"
            });
        }
		
		const user = results[0];
		
		const code = await generateUniqueCode();
		
		const insertQuery = `
            INSERT INTO DeleteAccountRequests  (Uid, Code)
            VALUES (?, ?)
        `;
		
		 await connection.promise().query(insertQuery, [userId, code]);
		
        const mailOptions = {
            from: 'filmbeadando2024@gmail.com',
            to: user.Email,
            subject: 'Account deletion',
            text: `Account deletion has been requested, your code is: ${code}, if it was not you, contact support immediately!`
        };

        transporter.sendMail(mailOptions, (err) => {
            if (err) {
                console.error(err);
                return res.status(500).json({ message: 'Failed to send email' });
            }
            res.json({ message: 'Verification code sent' });
        });
		
	} catch (err) {
		console.error('Error in request-delete-account:', err);
        res.status(500).json({ 
			success: false,
            message: userApiErrorMessage,
            error: err.message
		});
	}
});

app.post('/confirm-delete-user', authenticateToken , async (req, res) => {
	try {
		const { userId, password, email, code } = req.body;
		console.log('Received deletion confirm request for user: ', userId);
		
		  if (!userId) {
            return res.status(400).json({
                success: false,
				message: userApiErrorMessage,
                error: "UserId are missing"
            });
        }
		
		//Code validation
		const [rows] = await connection.promise().query(
            'SELECT Code, CreatedAt FROM DeleteAccountRequests WHERE Uid = ? ORDER BY CreatedAt DESC LIMIT 1',
            [userId]
        );
		
		 if (rows.length === 0) {
            return res.status(404).json({ error: 'No pending account deletion found' });
        }

        const request = rows[0];
		
		const createdAt = new Date(request.CreatedAt);
		const now = new Date();
		const diffMinutes = (now - createdAt) / 100 / 60; //ez 10 perc

		if (diffMinutes > 10) {
			return res.status(400).json({ message: 'Verification code expired' });
		}
		
        if (request.Code !== code) {
            return res.status(400).json({ message: 'Invalid verification code' });
        }
		//End
		
		//Email and password validation
		 const query = 'SELECT * FROM Users WHERE Email = ?';
        const [results] = await connection.promise().query(query, [email]);

        if (results.length === 0) {
            return res.status(401).json({ error: 'Email not found' });
        }

        const user = results[0];

        const isPasswordValid = await bcrypt.compare(password, user.PassHashed);
        if (!isPasswordValid) {
            return res.status(401).json({ error: 'Invalid password' });
        }
		
		const [activeSessions] = await connection.promise().query(
			`SELECT COUNT(*) AS count FROM sessions WHERE UserId = ? AND ExpiresAt > NOW()`,
			[userId]
		);

		if (activeSessions[0].count > 1) {
			return res.status(400).json({
				error: 'More than 1 active session.'
			});
		}

		//End
		
		await connection.promise().query(
			'INSERT INTO DeletedSales (Sid, Uid) SELECT Sid, Uid FROM Sales WHERE Uid = ?',
			[userId]
		);
		
		await connection.promise().query(
            'Delete FROM Sales WHERE Uid = ?',
			[userId]
        );
		
		await connection.promise().query(
			'INSERT INTO DeletedUsers (Uid) VALUES (?)',
			[userId]
		);
		
		await connection.promise().query(
            'Delete FROM Users WHERE Uid = ?',
			[userId]
        );
		
		await connection.promise().query(
            'DELETE FROM DeleteAccountRequests WHERE Uid = ?',
            [userId]
        );
		
		 res.json({ message: 'Account deleted successfully' });
		
		
		
	} catch (err) {
		console.error('Error in confirm-delete-account:', err);
        res.status(500).json({ 
			success: false,
            message: userApiErrorMessage,
            error: err.message
		});
	}
});


//Start the server
//app.listen(port, () => {
//    console.log(`Server running on http://localhost:${port}`);
//});


// Start the server
app.listen(port, "0.0.0.0", () => {
    console.log(`Server running on http://0.0.0.0:${port}`);
    console.log(`Local access:   http://localhost:${port}`);
    console.log(`LAN access:     http://192.168.1.85:${port}`);
});




