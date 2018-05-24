#pragma once

#include "DssFileFormat.h"
#include <vector>

namespace DssFileFormat {
	namespace v2 {

		enum {
			RELEASE_ID_1		= 1,
			LATEST_RELEASE_ID	= RELEASE_ID_1
		};
		LONG GetMaxIndexMarks(long releaseId=LATEST_RELEASE_ID);

		struct ICommonHeader {
			virtual ~ICommonHeader() {}

			virtual ICommonHeader*		duplicate() = 0;

			virtual void				set(const std::string& key, const std::string& value) = 0;
			virtual std::string			get(const std::string& key) = 0;
			virtual std::string			get_binary(const std::string& key) = 0;
			virtual common_header_t&	fields() = 0;
			virtual const common_header_t& fields() const = 0;
			virtual void				write(void *pDestination, size_t bytes, bool withFormatFraming=true) = 0;
			virtual const ICommonHeader* originalHeader() const = 0;

			virtual std::string			selfIdentifier(const char *newValue=0) = 0;
			virtual std::string			authorId(const char *newValue=0) = 0;
			virtual std::string			recStartDate(const char *newValue=0) = 0;
			virtual std::string			recStartTime(const char *newValue=0) = 0;
			virtual std::string			recEndDate(const char *newValue=0) = 0;
			virtual std::string			recEndTime(const char *newValue=0) = 0;
			virtual std::string			lengthTime(const char *newValue=0) = 0;
			virtual std::string			typistId(const char *newValue=0) = 0;
			virtual std::string			workTypeId(const char *newValue=0) = 0;
			virtual std::string			optionItemName1(const char *newValue=0) = 0;
			virtual std::string			optionItemId1(const char *newValue=0) = 0;
			virtual std::string			optionItemName2(const char *newValue=0) = 0;
			virtual std::string			optionItemId2(const char *newValue=0) = 0;
			virtual std::string			optionItemName3(const char *newValue=0) = 0;
			virtual std::string			optionItemId3(const char *newValue=0) = 0;
			virtual std::string			notes(const char *newValue=0) = 0;
			virtual std::vector<dword_t> indexMarks() = 0;
			virtual void				setIndexMarks(std::vector<dword_t> marks) = 0;
		};

		ICommonHeader* createHeader(const char *fmt ="wav");
		ICommonHeader* createHeaderFromMemory(const void *pMemory, size_t bytes, bool autoUpdateFromV1 = true);
		ICommonHeader* createHeaderFromV1(const void *pVersion1Raw, size_t bytes, const char *containerFormat="wav");

		void inheritFromHeader(ICommonHeader *dest, const ICommonHeader *src, bool isDirty = false);

#ifdef DSS_ELEMENT_H__
		void writeToV1(ICommonHeader* pch, DssFileFormat::DssCommonHeader& ch, DssFileFormat::DssHeader *secondHeader=0, DssFileFormat::DssHeader *thirdHeader=0);
//		void writeToV1(ICommonHeader* pch, DssFileFormat::DssScanner& scanner);
#endif

		void set_date_time(ascii6_t& theDate, ascii6_t& theTime, std::tm *timeStamp=0);

		std::string to_text(const ICommonHeader* header);
	}
}
