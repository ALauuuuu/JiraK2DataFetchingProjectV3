import React, { useState, useRef } from 'react';
import './Form.css';
import setup_param from '../config/config.jsx';
import axios from 'axios';

const Form = () => {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1; // JavaScript months are 0-indexed
  const [emailUser, setEmailUser] = useState('');
  const [emailDomain, setEmailDomain] = useState(setup_param.default_email_domain);
  const [year, setYear] = useState(currentYear);
  const [batchNo, setBatchNo] = useState(currentMonth);
  const [emailError, setEmailError] = useState('');
  const [apiResponse, setApiResponse] = useState({});
  const [submitted, setSubmitted] = useState(false); //bool

  const handleEmailUserChange = (e) => {
    const newEmail = e.target.value;
    setEmailUser(newEmail);
  };

  const handleEmailDomainChange = (e) =>{
    const newEmailDomain = e.target.value;
    setEmailDomain(newEmailDomain);
  }

  const incrementYear = () => {
    setYear(prev => prev < currentYear + 1 ? prev + 1 : prev);
  };

  const decrementYear = () => {
    setYear(prev => prev > currentYear - 1 ? prev - 1 : prev);
  };

  const incrementBatchNo = () => {
    setBatchNo(prev => prev < 99 ? prev + 1 : prev);
  };

  const decrementBatchNo = () => {
    setBatchNo(prev => prev > 1 ? prev - 1 : prev);
  };

  const handleGetBiweeklyEmail = (e) => {
    e.preventDefault();
    const completeEmail = `${emailUser}@${emailDomain}`;
    if(confirm(`Confirm to receive "${year}-${batchNo.toString().padStart(2, '0')}" bi-weekly email at\n"${completeEmail}"?`)) {
      setSubmitted(true);
      const yearBatchNo = `${year}-${batchNo.toString().padStart(2, '0')}`;
      setApiResponse(setup_param.default_loading_message);
      axios.get(`${setup_param.backend_api_endpoint}/v1/receive-biweekly-email/email=${completeEmail}&batch=${yearBatchNo}`)
        .then(res => {
          setApiResponse(res.data);
          setSubmitted(false);
        }).catch(err => {
          console.error('Error:', err);
          setSubmitted(false);
        });
      // console.log('Submit Get Biweekly Email:', { email: completeEmail, yearBatchNo: `${year}-${batchNo.toString().padStart(2, '0')}` });
    }
  };

  // const handleGetBiweeklyFiles = (e) => {
  //   e.preventDefault();
  //   if(confirm(`Confirm to download "${year}-${batchNo.toString().padStart(2, '0')}" bi-weekly files?`)) {
  //     setSubmitted(true);
  //     emailRef.current.click();
  //     const cntDown = setup_param.default_countdown_sec;
  //     setTimer(cntDown);
  //     const intervelID = setInterval(() => {
  //       setTimer(prev => prev - 1 >= 0 ? prev - 1 : null);
  //     }, 1000);
  //     setTimeout(() => {
  //       clearInterval(intervelID);
  //       setTimer(null);
  //       setSubmitted(false);
  //     }, 1000 * cntDown);
  //   }
  // };

  const handleGetBiweeklyFiles = (e) => {
    e.preventDefault();
    const yearBatchNo = `${year}-${batchNo.toString().padStart(2, '0')}`;
    if(confirm(`Confirm to download "${yearBatchNo}" bi-weekly files?`)) {
      setSubmitted(true);
      setApiResponse(setup_param.default_loading_message);      
      // Use axios to download the file with blob response type
      axios.get(`${setup_param.backend_api_endpoint}/v1/get-zip-by-batch/${yearBatchNo}`, {
        responseType: 'blob' // Important for binary data
      })
      .then(response => {
        // Check if response data is valid
        if (!response.data || response.data.size === 0){
          setApiResponse({ 
            status: 'error', 
            message: `Failed to download ${yearBatchNo}.zip. Something might have gone wrong in the backend.` 
          });
          return;
        }

        // Create a blob from the response data
        const blob = new Blob([response.data]);
        
        // Create a URL for the blob
        const url = window.URL.createObjectURL(blob);
        
        // Create a temporary anchor element to trigger download
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `${yearBatchNo}.zip`);
        
        // Append to document, click, and remove
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        // Clean up the URL object
        window.URL.revokeObjectURL(url);
        
        setApiResponse({ status: 'success', message: `Successfully downloaded bi-weekly files for ${yearBatchNo}` });
      })
      .catch(error => {
        console.error('Error downloading files:', error);
        setApiResponse({ status: 'error', message: 'Failed to download files. Please try again later.' });
      })
      .finally(() => {
        // clearInterval(intervalID);
        // setTimer(null);
        setSubmitted(false);
      });
    }
  };

  const handleGetUrgSerSpeEmail = (e) => {
    e.preventDefault();
    const completeEmail = `${emailUser}@${emailDomain}`;
    if (confirm(`Confirm to receive urgent/service/special email at\n"${completeEmail}"?`)) {
      setSubmitted(true);
      setApiResponse(setup_param.default_loading_message);
      axios.get(`${setup_param.backend_api_endpoint}/v1/receive-urgent-service-special-email/email=${completeEmail}`)
        .then(res => {
          setApiResponse(res.data);
          setSubmitted(false);
        }).catch(err => {
          console.error('Error:', err);
          setSubmitted(false);
        });
      // console.log('Submit Get Urgent/Service/Special Email:', completeEmail);
    }
  };

    const handleGetUrgSerSpeExcel = (e) => {
    e.preventDefault();
    const completeEmail = `${emailUser}@${emailDomain}`;
    if(confirm(`Confirm to send urgent/service/special excel?`)) {
      setSubmitted(true);
      setApiResponse(setup_param.default_loading_message);      

//       axios.get(`${setup_param.backend_api_endpoint}/v1/receive-urgent-service-special-excel/email=${completeEmail}`)
//         .then(res => {
//           setApiResponse(res.data);
//           setSubmitted(false);
//         }).catch(err => {
//           console.error('Error:', err);
//           setSubmitted(false);
//         });
        
      axios.get(`${setup_param.backend_api_endpoint}/v1/get-urgent-service-special-excel/email=${completeEmail}`, {
        responseType: 'blob' // Important for binary data
      })
      .then(response => {
        // Check if response data is valid
        if (!response.data || response.data.size === 0){
          setApiResponse({
            status: 'error',
            message: `Failed to send Excel file Something might have gone wrong in the backend.`
          });
          return;
        }

//         const byteArray = new Uint8Array(response.data);
//         const blob = new Blob([response.data]);
//
//         // Create a URL for the blob
//         const url = window.URL.createObjectURL(blob);
//
//         const link = document.createElement('a');
//         link.href = url;
//         link.setAttribute('download', `temp.xlsx`);
//
//         document.body.appendChild(link);
//         link.click();
//         document.body.removeChild(link);
//
//         window.URL.revokeObjectURL(url);

        setApiResponse({ status: 'success', message: `Successfully sent Excel file` });
        setSubmitted(false);
      })
      .catch(error => {
        console.error('Error sending files:', error);
        setApiResponse({ status: 'error', message: 'Failed to send files. Please try again later.' });
      })
      .finally(() => {
        // clearInterval(intervalID);
        // setTimer(null);
        setSubmitted(false);
      });


    }
  };

  const handleGetUrgSerSpeSQLFiles = (e) => {
    e.preventDefault();
    if(confirm(`Confirm to download SQL files?`)) {
      setSubmitted(true);
      setApiResponse(setup_param.default_loading_message);      
      // Use axios to download the file with blob response type
      axios.get(`${setup_param.backend_api_endpoint}/v1/get-urgent-service-special-verify-sql-zip`, {
        responseType: 'blob' // Important for binary data
      })
      .then(response => {
        // Check if response data is valid
        if (!response.data || response.data.size === 0){
          setApiResponse({ 
            status: 'error', 
            message: `Failed to download sql.zip. Something might have gone wrong in the backend.` 
          });
          return;
        }

        // Create a blob from the response data
        const blob = new Blob([response.data]);
        
        // Create a URL for the blob
        const url = window.URL.createObjectURL(blob);

        let newDate = new Date()
        // let date = newDate.getDate();
        // let month = newDate.getMonth() + 1;
        let date = newDate.getDate().toString().padStart(2, '0');
        let month = (newDate.getMonth() + 1).toString().padStart(2, '0');
        let year = newDate.getFullYear();
        
        // Create a temporary anchor element to trigger download
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `Verify_UrgSerSpeSQL_${year}${month}${date}.zip`);
        
        // Append to document, click, and remove
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        // Clean up the URL object
        window.URL.revokeObjectURL(url);
        
        setApiResponse({ status: 'success', message: `Successfully downloaded sql files` });
      })
      .catch(error => {
        console.error('Error downloading files:', error);
        setApiResponse({ status: 'error', message: 'Failed to download files. Please try again later.' });
      })
      .finally(() => {
        // clearInterval(intervalID);
        // setTimer(null);
        setSubmitted(false);
      });
    }
    
  };

      // Use axios to download the file with blob response type
      // axios.get(`${setup_param.backend_api_endpoint}/v1/get-urgent-service-special-excel/email=${completeEmail}`, {
      //   responseType: 'blob' // Important for binary data
      // })

//       axios.get(`${setup_param.backend_api_endpoint}/v1/get-urgent-service-special-excel/email=${completeEmail}`)
//         .then(res => {
//           setApiResponse(res.data);
//           setSubmitted(false);
//         }).catch(err => {
//           console.error('Error:', err);
//           setSubmitted(false);
//         });
//       Use axios to download the file with blob response type

  return (
    <div className="form-container">
      <h2 className="form-header">Promotion Updates Self-Service</h2>
      <form>
        <div className="form-top-section">
          <div className="form-group">
            <label htmlFor="email">Email Address (Optional):</label>
            <input
              type="email"
              id="email"
              value={emailUser}
              onChange={handleEmailUserChange}
              // placeholder="jasper.chan"
              className={emailError ? 'error' : ''}
              autoComplete='username'
            />
            <input className='name-domain-sep' value='@' readOnly></input>
            <input
              id="email-domain"
              value={emailDomain}
              onChange={handleEmailDomainChange}
              placeholder="e.g. ha.org.hk"
            />
            <div className="message-container">
              {emailError && <div className="error-message">{emailError}</div>}
            </div>
          </div>
        </div>

        <div className="form-middle-section">
          <div className="year-batch-inputs-container">
            <label>Bi-weekly Information:</label>
            <div className="year-batch-input-group">
              <label htmlFor="year">Year:</label>
              <div className="counter-input">
                <button 
                  type="button" 
                  className="counter-btn" 
                  onClick={decrementYear}
                  disabled={year <= currentYear - 1}
                >
                  -
                </button>
                <input
                  type="text"
                  id="year"
                  value={year}
                  readOnly
                />
                <button 
                  type="button" 
                  className="counter-btn" 
                  onClick={incrementYear}
                  disabled={year >= currentYear + 1}
                >
                  +
                </button>
              </div>
            </div>

            <div className="year-batch-input-group">
              <label htmlFor="batchNo">Batch:</label>
              <div className="counter-input">
                <button 
                  type="button" 
                  className="counter-btn" 
                  onClick={decrementBatchNo}
                  disabled={batchNo <= 1}
                >
                  -
                </button>
                <input
                  type="text"
                  id="batchNo"
                  value={batchNo.toString().padStart(2, '0')}
                  readOnly
                />
                <button 
                  type="button" 
                  className="counter-btn" 
                  onClick={incrementBatchNo}
                  disabled={batchNo >= 99}
                >
                  +
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="form-bottom-section">
          <div className="button-group">
            <button 
              type="button" 
              onClick={handleGetBiweeklyEmail}
              disabled={!emailUser || !emailDomain || submitted}
              className="submit-btn get-bi-email"
              title={emailUser && emailDomain ? `Click to receive ${year}-${batchNo.toString().padStart(2, '0')} bi-weekly email at ${emailUser}@${emailDomain}`: undefined}
            >
              Get Bi-weekly Email
            </button>
            <button 
              type="button" 
              onClick={handleGetUrgSerSpeEmail}
              disabled={!emailUser || !emailDomain || submitted}
              className="submit-btn get-urg-ser-spe-email"
              title={emailUser && emailDomain ? `Click to receive urgent/service/special email at ${emailUser}@${emailDomain}` : undefined}
            >
              Get Urgent/Service/Special Email
            </button>
            <button 
              type="button" 
              onClick={handleGetBiweeklyFiles}
              className="submit-btn get-bi-files"
              disabled={submitted}
            >
              Get Bi-weekly Files
            </button>
            <button 
              type="button" 
              onClick={handleGetUrgSerSpeExcel}
              disabled={!emailUser || !emailDomain || submitted}
              className="submit-btn get-urg-ser-spe-excel"
              title={emailUser && emailDomain ? `Click to receive urgent/service/special excel email at ${emailUser}@${emailDomain}` : undefined}
            >
              Get Urgent/Service/Special Excel
            </button>
            <button
             type="button"
             className="submit-btn get-bi-weekly-sql"
             disabled={submitted}
            >
              Get Bi-weekly SQL
            </button>
            <button
              type="button"
              onClick={handleGetUrgSerSpeSQLFiles}
              className="submit-btn get-urg-ser-spe-sql"
              disabled={submitted}
            >
              Get Urgent/Service/Special SQL
            </button>
          </div>
        </div>
        {apiResponse && <div className="response-message">
          <span className={apiResponse.status === 'error' ? 'error-api-resp' : 'success-api-resp'}>{apiResponse.message}</span>
          </div>}
        <div className='disclaimer'>
          <p>
            Disclaimer: Please make sure all the inputs, including the email address and the bi-weekly information, if applicable, are correct. The developer of this tool holds no responsibility for any failure of receiving the intended emails and/or files.
          </p>
        </div>
      </form>
    </div>
  );
};

export default Form;
